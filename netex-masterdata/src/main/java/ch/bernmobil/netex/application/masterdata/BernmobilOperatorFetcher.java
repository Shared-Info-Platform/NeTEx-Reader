package ch.bernmobil.netex.application.masterdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.bernmobil.netex.application.masterdata.model.Operator;

public class BernmobilOperatorFetcher {

	private static final Logger logger = LoggerFactory.getLogger(BernmobilOperatorFetcher.class);

	private final String url;
	private final int operatorsPerRequest;
	private final ObjectMapper mapper;

	public BernmobilOperatorFetcher(String url, int operatorsPerRequest) {
		this.mapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
		this.mapper.registerModule(new JavaTimeModule());
		this.url = url;
		this.operatorsPerRequest = operatorsPerRequest;
	}

	public List<Operator> fetchOperators() throws IOException, URISyntaxException {
		final List<Operator> result = new ArrayList<>();
		for (int pageNumber = 0; true; ++pageNumber) {
			final String json = fetchJson(pageNumber, operatorsPerRequest);
			if (json == null) {
				break;
			}

			final BernmobilPage page = mapJsonToObject(json);
			if (page == null || page.content == null || page.content.isEmpty()) {
				break;
			}

			result.addAll(page.content.stream().map(this::convertOperator).filter(Objects::nonNull).collect(Collectors.toList()));

			logger.info("fetched a total of {} operators", result.size());
		}
		return result;
	}

	String fetchJson(int pageNumber, int limit) throws IOException, URISyntaxException {
		if (url == null || url.isEmpty()) {
			logger.info("url is missing, fetching operators is disabled");
			return null;
		}

		String urlWithParams = url;
		urlWithParams += "?page=" + pageNumber;
		urlWithParams += "&size=" + limit;

		logger.info("fetch json from {}", urlWithParams);
		final HttpURLConnection conn = (HttpURLConnection) new URI(urlWithParams).toURL().openConnection();
		conn.setRequestMethod("GET");

		final InputStream dataStream = conn.getInputStream();
		final StringWriter stringWriter = new StringWriter();
		try (final Reader reader = new InputStreamReader(dataStream, StandardCharsets.UTF_8)) {
			reader.transferTo(stringWriter);
		}
		final String json = stringWriter.toString();
		dataStream.close();

		logger.info("fetch done");

		return json;
	}

	private BernmobilPage mapJsonToObject(String json) throws IOException {
		return mapper.readValue(json, BernmobilPage.class);
	}

	private Operator convertOperator(BernmobilOperator operator) {
		final Operator result = new Operator();
		result.setOperatorId(operator.operatorId);
		if (operator.organisationNumber != null) {
			result.setOrganisationNumber(operator.organisationNumber.toString());
		}
		result.setValidFrom(operator.validFrom);
		result.setValidTo(operator.validTo);
		return result;
	}

	private static class BernmobilPage {
		public List<BernmobilOperator> content;
	}

	private static class BernmobilOperator {
		public String operatorId;
		public Integer organisationNumber;
		public LocalDate validFrom;
		public LocalDate validTo;
	}
}
