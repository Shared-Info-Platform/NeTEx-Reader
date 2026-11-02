package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import ch.bernmobil.netex.application.masterdata.model.Operator;

public class BernmobilOperatorFetcherIntegrationTest {

	private WireMockServer server;
	private String json1 = """
			{
			  "page": 0,
			  "content": [
			    {
			      "organisationNumber": 820,
			      "validFrom": "1900-01-01",
			      "validTo": "9999-12-31",
			      "operatorId": "85:820"
			    }
			  ],
			  "size": 1,
			  "totalElements": 2,
			  "totalPages": 2
			}""";

	private String json2 = """
			{
			  "page": 1,
			  "content": [
			    {
			      "organisationNumber": 801,
			      "validFrom": "1900-01-01",
			      "validTo": "9999-12-31",
			      "operatorId": "85:801"
			    }
			  ],
			  "size": 1,
			  "totalElements": 2,
			  "totalPages": 2
			}""";

	private String json3 = """
			{
			  "page": 2,
			  "content": [],
			  "size": 1,
			  "totalElements": 2,
			  "totalPages": 2
			}""";

	@BeforeEach
	public void before() throws Exception {
		server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		server.stubFor(WireMock.get("/?page=0&size=1").willReturn(WireMock.okJson(json1)));
		server.stubFor(WireMock.get("/?page=1&size=1").willReturn(WireMock.okJson(json2)));
		server.stubFor(WireMock.get("/?page=2&size=1").willReturn(WireMock.okJson(json3)));
		server.start();
	}

	@AfterEach
	public void after() throws Exception {
		server.stop();
	}

	@Test
	public void test() throws IOException, URISyntaxException {
		final BernmobilOperatorFetcher operatorFetcher = new BernmobilOperatorFetcher(server.baseUrl() + "/", 1);
		final List<Operator> operators = operatorFetcher.fetchOperators();
		assertThat(operators).hasSize(2);
		assertThat(operators.get(0).getOperatorId()).isEqualTo("85:820");
		assertThat(operators.get(1).getOperatorId()).isEqualTo("85:801");
	}
}
