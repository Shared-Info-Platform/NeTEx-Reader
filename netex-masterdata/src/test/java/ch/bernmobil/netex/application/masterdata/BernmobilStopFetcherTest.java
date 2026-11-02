package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.bernmobil.netex.application.masterdata.model.Stop;
import ch.bernmobil.netex.application.masterdata.model.StopPoint;

public class BernmobilStopFetcherTest {

	private static class BernmobilStopFetcherWithMockedJson extends BernmobilStopFetcher {
		private final String json;
		private boolean firstCall = true;

		public BernmobilStopFetcherWithMockedJson(String json) {
			super(null, 1);
			this.json = json;
		}

		@Override
		String fetchJson(int offset, int limit, String version) {
			if (firstCall) {
				firstCall = false;
				return json;
			} else {
				return null;
			}
		}
	}

	@Test
	public void testFetchEmptyList() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).isEmpty();
	}

	@Test
	public void testFetchSingleStopWithFullInformation() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"didokNumberFull\": 8578520," +
				"        \"sloid\": \"ch:1:sloid:78520\"" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		final Stop stop = stops.get(0);
		assertThat(stop.getDidok()).isEqualTo("8578520");
		assertThat(stop.getSloid()).isEqualTo("ch:1:sloid:78520");
		assertThat(stop.getStopPoints()).isEmpty();
	}

	@Test
	public void testFetchSingleStopWithOnlyUicCode() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"didokNumberFull\": 8578520" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		final Stop stop = stops.get(0);
		assertThat(stop.getDidok()).isEqualTo("8578520");
		assertThat(stop.getSloid()).isNull();
		assertThat(stop.getStopPoints()).isEmpty();
	}

	@Test
	public void testFetchSingleStopWithOnlySloid() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"sloid\": \"ch:1:sloid:78520\"" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		final Stop stop = stops.get(0);
		assertThat(stop.getSloid()).isEqualTo("ch:1:sloid:78520");
		assertThat(stop.getDidok()).isNull();
		assertThat(stop.getStopPoints()).isEmpty();
	}

	@Test
	public void testFetchMultipleStops() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"didokNumberFull\": 8578520," +
				"        \"sloid\": \"ch:1:sloid:78520\"" +
				"    }," +
				"    {" +
				"        \"didokNumberFull\": 8578521" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(2);
		assertThat(stops.get(0).getDidok()).isEqualTo("8578520");
		assertThat(stops.get(1).getDidok()).isEqualTo("8578521");
	}

	@Test
	public void testFetchStopPointWithOnlyUicCode() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"didokNumberFull\": 8578520," +
				"        \"stopPoints\": [" +
				"            {" +
				"                \"didokNumberFull\": \"857852001\"" +
				"            }" +
				"        ]" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		assertThat(stops.get(0).getStopPoints()).hasSize(1);
		final StopPoint stopPoint = stops.get(0).getStopPoints().get(0);
		assertThat(stopPoint.getDidok()).isEqualTo("857852001");
		assertThat(stopPoint.getSloid()).isNull();
	}

	@Test
	public void testFetchStopPointWithOnlySloid() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"didokNumberFull\": 8578520," +
				"        \"stopPoints\": [" +
				"            {" +
				"                \"sloid\": \"ch:1:sloid:78520:0:01\"" +
				"            }" +
				"        ]" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		assertThat(stops.get(0).getStopPoints()).hasSize(1);
		final StopPoint stopPoint = stops.get(0).getStopPoints().get(0);
		assertThat(stopPoint.getDidok()).isNull();
		assertThat(stopPoint.getSloid()).isEqualTo("ch:1:sloid:78520:0:01");
	}

	@Test
	public void testFetchMultipleStopPoints() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"didokNumberFull\": 8578520," +
				"        \"stopPoints\": [" +
				"            {" +
				"                \"didokNumberFull\": \"857852001\"" +
				"            }," +
				"            {" +
				"                \"didokNumberFull\": \"857852002\"" +
				"            }" +
				"        ]" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		assertThat(stops.get(0).getStopPoints()).hasSize(2);
	}

	@Test
	public void testFetchStopWithNoInformation() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
	}

	@Test
	public void testFetchStopPointWithNoInformation() throws IOException, URISyntaxException {
		final String json = "" +
				"[" +
				"    {" +
				"        \"didokNumberFull\": 8578520," +
				"        \"stopPoints\": [" +
				"            {" +
				"            }" +
				"        ]" +
				"    }" +
				"]";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		assertThat(stops.get(0).getStopPoints()).hasSize(1);
	}

	@Test
	// just a smoke test to see if the code can handle real data including ignored attributes (without testing the values in depth)
	public void testFetchWithRealData() throws IOException, URISyntaxException {
		final String json = "" +
				"{" +
				"    \"coordinatesWgs84\": {" +
				"        \"latitude\": 47.43035630723," +
				"        \"longitude\": 9.04623545481" +
				"    }," +
				"    \"designationOfficial\": \"Kirchberg SG, Lamperswil\"," +
				"    \"didokNumberFull\": 8578520," +
				"    \"id\": 2149821," +
				"    \"sloid\": \"ch:1:sloid:78520\"," +
				"    \"stopPoints\": [" +
				"        {" +
				"            \"coordinatesWgs84\": {" +
				"                \"latitude\": 47.43045795833," +
				"                \"longitude\": 9.04628643936" +
				"            }," +
				"            \"designationPublic\": \"B\"," +
				"            \"didokNumberFull\": \"857852002\"," +
				"            \"id\": 668344," +
				"            \"sloid\": \"ch:1:sloid:78520:0:02\"" +
				"        }," +
				"        {" +
				"            \"coordinatesWgs84\": {" +
				"                \"latitude\": 47.43023311017," +
				"                \"longitude\": 9.04625050675" +
				"            }," +
				"            \"designationPublic\": \"A\"," +
				"            \"didokNumberFull\": \"857852001\"," +
				"            \"id\": 668356," +
				"            \"sloid\": \"ch:1:sloid:78520:0:01\"" +
				"        }" +
				"    ]," +
				"    \"version\": \"66139ebf86180c62ca5aeea9\"" +
				"}";

		final BernmobilStopFetcher fetcher = new BernmobilStopFetcherWithMockedJson(json);
		final List<Stop> stops = fetcher.fetchStops();
		assertThat(stops).hasSize(1);
		assertThat(stops.get(0).getStopPoints()).hasSize(2);
	}
}
