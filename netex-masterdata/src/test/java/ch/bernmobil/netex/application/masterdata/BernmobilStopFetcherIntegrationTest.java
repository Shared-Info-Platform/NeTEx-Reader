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

import ch.bernmobil.netex.application.masterdata.model.Stop;

public class BernmobilStopFetcherIntegrationTest {

	private WireMockServer server;
	private String json1 = "" +
			"[" +
			"    {" +
			"        \"didokNumberFull\": 8578520," +
			"        \"designationOfficial\": \"Kirchberg SG, Lamperswil\"," +
			"        \"coordinatesWgs84\": {" +
			"            \"latitude\": 47.43035630724," +
			"            \"longitude\": 9.04623545481" +
			"        }," +
			"        \"sloid\": \"ch:1:sloid:78520\"," +
			"        \"version\": \"x\"" +
			"    }" +
			"]";
	private String json2 = "" +
			"[" +
			"    {" +
			"        \"didokNumberFull\": 8578521," +
			"        \"stopPoints\": [" +
			"            {" +
			"                \"didokNumberFull\": \"857852101\"" +
			"            }," +
			"            {" +
			"                \"didokNumberFull\": \"857852102\"" +
			"            }" +
			"        ]" +
			"    }" +
			"]";

	@BeforeEach
	public void before() throws Exception {
		server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		server.stubFor(WireMock.get("/?offset=0&limit=1").willReturn(WireMock.okJson(json1)));
		server.stubFor(WireMock.get("/?offset=1&limit=1&version=x").willReturn(WireMock.okJson(json2)));
		server.stubFor(WireMock.get("/?offset=2&limit=1&version=x").willReturn(WireMock.okJson("[]")));
		server.start();
	}

	@AfterEach
	public void after() throws Exception {
		server.stop();
	}

	@Test
	public void test() throws IOException, URISyntaxException {
		final BernmobilStopFetcher stopFetcher = new BernmobilStopFetcher(server.baseUrl() + "/", 1);
		final List<Stop> stops = stopFetcher.fetchStops();
		assertThat(stops).hasSize(2);
		assertThat(stops.get(0).getStopPoints()).hasSize(0);
		assertThat(stops.get(1).getStopPoints()).hasSize(2);
	}
}
