package ch.bernmobil.netex.haltelog.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.bernmobil.netex.haltelog.properties.ElasticProperties;
import ch.bernmobil.netex.haltelog.properties.SecurityProperties;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer.Scheme;
import org.elasticsearch.client.sniff.NodesSniffer;
import org.elasticsearch.client.sniff.SniffOnFailureListener;
import org.elasticsearch.client.sniff.Sniffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * Note: Keep this in sync with ElasticSingletons in SIP-Hub.
 */
public class ElasticSingletons implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(ElasticSingletons.class);

  private ElasticsearchClient client;
  private BulkIngester<Void> bulkIngester;
  private @Nullable Sniffer sniffer;

  public ElasticSingletons(ElasticProperties elasticProperties) {
    initialize(elasticProperties);
  }

  public ElasticsearchClient getClient() {
    return client;
  }

  public BulkIngester<Void> getBulkIngester() {
    return bulkIngester;
  }

  private void initialize(ElasticProperties elasticProperties) {
    initElasticsearchClient(elasticProperties);
    initBulkIngester(client, elasticProperties);
  }

  private void initElasticsearchClient(ElasticProperties elasticProperties) {
    SniffOnFailureListener sniffOnFailureListener = null;
    boolean sniffingEnabled = elasticProperties.isSniffingEnabled();
    if (sniffingEnabled) {
      sniffOnFailureListener = new SniffOnFailureListener();
    }

    RestClient restClient = restClient(elasticProperties, sniffOnFailureListener);
    JsonpMapper jsonpMapper = jsonpMapper();
    client = new ElasticsearchClient(new RestClientTransport(restClient, jsonpMapper));

    if (sniffingEnabled) {
      sniffer = sniffer(restClient, sniffOnFailureListener, elasticProperties);
    }
  }

  private void initBulkIngester(ElasticsearchClient esClient, ElasticProperties elasticProperties) {
    BulkListener<Void> bulkListener = bulkListener();

    // From the Elastic documentation:
    // The ingester will send a bulk request when one of the following criteria is met:
    // - The number of operations exceeds a maximum
    // - The bulk request size in bytes exceeds a maximum
    // - A delay since the last request has expired
    bulkIngester = BulkIngester.of(b -> b.client(esClient)
                                         .listener(bulkListener)
                                         .maxOperations(elasticProperties.getBatchMaxCount())
                                         .maxSize(elasticProperties.getBatchMaxSizeBytes())
                                         .flushInterval(elasticProperties.getBatchFlushInterval().toMillis(),
                                             TimeUnit.MILLISECONDS));
  }

  private BulkListener<Void> bulkListener() {
    return new ElasticSearchListener();
  }

  private static class ElasticSearchListener implements BulkListener<Void> {

    @Override
    public void beforeBulk(long executionId, BulkRequest request, List<Void> contexts) {
      // skip
    }

    /**
     * Called after successful sending (response could have errors though)
     */
    @Override
    public void afterBulk(long executionId, BulkRequest request, List<Void> contexts, BulkResponse response) {
      if (response.errors()) {
        List<ErrorCause> errorCauses = response.items().stream().map(BulkResponseItem::error).toList();
        logger.error("BulkResponse has errors: executionId={}, responseErrors={}, contexts={}",
            executionId, errorCauses, contexts);
      }
    }

    /**
     * Called on sending error
     */
    @Override
    public void afterBulk(long executionId, BulkRequest request, List<Void> contexts, Throwable failure) {
      if (failure instanceof ConnectException) {
        // Suppress stacktrace for 'ConnectException'. It would just bloat the log.
        logger.error("BulkRequest could not be sent. ConnectException: \"{}\"", failure.getMessage());
      } else {
        logger.error("BulkRequest could not be sent.", failure);
      }
    }
  }

  private Sniffer sniffer(RestClient restClient, SniffOnFailureListener failureListener,
      ElasticProperties elasticProperties) {
    Scheme scheme = elasticProperties.getSecurity().isUseHttps() ? Scheme.HTTPS : Scheme.HTTP;
    NodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(
        restClient,
        ElasticsearchNodesSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT,
        scheme);

    Sniffer result = Sniffer.builder(restClient).setNodesSniffer(nodesSniffer).build();
    failureListener.setSniffer(result);
    return result;
  }

  private JsonpMapper jsonpMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    // Add support for java.time types
    objectMapper.registerModule(new JavaTimeModule());

    // Serialize dates as ISO-8601 strings instead of timestamps.
    // (For compatibility with elasticsearch "date" type fields)
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new JacksonJsonpMapper(objectMapper);
  }

  private RestClient restClient(ElasticProperties elasticProperties,
      @Nullable SniffOnFailureListener sniffOnFailureListener) {
    SecurityProperties securityProperties = elasticProperties.getSecurity();
    HttpHost[] httpHosts = createHosts(elasticProperties);

    // Set up the REST client
    RestClientBuilder builder = RestClient.builder(httpHosts);

    if (securityProperties.isUseHttps()) {
      // Credentials should only be sent when using https
      String apiKey = securityProperties.getApiKey();
      if (apiKey == null || apiKey.isEmpty()) {
        logger.warn("API key is empty");
      }
      builder.setDefaultHeaders(new Header[]{
          new BasicHeader("Authorization", "ApiKey " + apiKey)
      });

      builder.setHttpClientConfigCallback(
          httpClientBuilder -> httpClientConfigCallback(httpClientBuilder, elasticProperties));
    }

    if (sniffOnFailureListener != null) {
      builder.setFailureListener(sniffOnFailureListener);
    }

    return builder.build();
  }

  private HttpHost[] createHosts(ElasticProperties elasticProperties) {
    String scheme = elasticProperties.getSecurity().isUseHttps() ? "https" : "http";

    List<HttpHost> hosts = new ArrayList<>();
    for (String host : elasticProperties.getHosts()) {
      String fullHost = scheme + "://" + host;
      hosts.add(HttpHost.create(fullHost));
    }
    return hosts.toArray(new HttpHost[0]);
  }

  private HttpAsyncClientBuilder httpClientConfigCallback(HttpAsyncClientBuilder httpClientBuilder,
      ElasticProperties elasticProperties) {
    httpClientBuilder.setSSLContext(createSSLContext(elasticProperties));
    return httpClientBuilder;
  }

  private SSLContext createSSLContext(ElasticProperties elasticProperties) {
    try {
      logger.info("create custom ssl context");
      SecurityProperties security = elasticProperties.getSecurity();
      final SSLContextBuilder sslBuilder = SSLContexts.custom();

      if (security.getTrustStorePath() != null) {
        final KeyStore trustStore = loadKeyStore("truststore",
            security.getTrustStorePath(),
            security.getTrustStorePassword(),
            security.getTrustStoreType());
        sslBuilder.loadTrustMaterial(trustStore, null);
      }

      if (security.getKeyStorePath() != null) {
        final KeyStore keyStore = loadKeyStore("keystore",
            security.getKeyStorePath(),
            security.getKeyStorePassword(),
            security.getKeyStoreType());
        sslBuilder.loadKeyMaterial(keyStore, security.getKeyStorePassword().toCharArray());
      }

      return sslBuilder.build();
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException("could not create ssl context for elastic", e);
    } catch (UnrecoverableKeyException | KeyStoreException | CertificateException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private KeyStore loadKeyStore(String name, String path, String password, String type)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    logger.info("loast custom " + name + " with path " + path);
    if (password == null) {
      throw new IllegalArgumentException("password for " + name + " is not defined");
    }

    final Path keyStorePath = Paths.get(new File(path).toURI());
    final KeyStore keyStore = KeyStore.getInstance(type);

    try (InputStream is = Files.newInputStream(keyStorePath)) {
      keyStore.load(is, password.toCharArray());
    }

    return keyStore;
  }

  @Override
  public void close() throws IOException {
    if (bulkIngester != null) {
      bulkIngester.close();
    }
    if (sniffer != null) {
      sniffer.close();
    }
    if (client != null) {
      client.close();
    }
  }
}
