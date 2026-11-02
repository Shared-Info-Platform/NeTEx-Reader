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

import ch.bernmobil.netex.application.masterdata.model.Line;

public class BernmobilLineFetcherIntegrationTest {

	private WireMockServer server;
	private String json1 = """
			{
			  "page": 0,
			  "content": [
			    {
			      "linienId": "85:820:1",
			      "slnid": "ch:1:slnid:1234",
			      "number": "1"
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
			      "linienId": "85:820:2",
			      "slnid": "ch:1:slnid:2345",
			      "number": "2"
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
		server.stubFor(WireMock.get("/?page=0&size=1&withReferencedEntities=true&valid=true&isSubline=false").willReturn(WireMock.okJson(json1)));
		server.stubFor(WireMock.get("/?page=1&size=1&withReferencedEntities=true&valid=true&isSubline=false").willReturn(WireMock.okJson(json2)));
		server.stubFor(WireMock.get("/?page=2&size=1&withReferencedEntities=true&valid=true&isSubline=false").willReturn(WireMock.okJson(json3)));
		server.start();
	}

	@AfterEach
	public void after() throws Exception {
		server.stop();
	}

	@Test
	public void test() throws IOException, URISyntaxException {
		final BernmobilLineFetcher lineFetcher = new BernmobilLineFetcher(server.baseUrl() + "/", 1);
		final List<Line> lines = lineFetcher.fetchLines();
		assertThat(lines).hasSize(2);
		assertThat(lines.get(0).getLineId()).isEqualTo("85:820:1");
		assertThat(lines.get(1).getLineId()).isEqualTo("85:820:2");
	}
}
