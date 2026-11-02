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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import ch.bernmobil.netex.application.masterdata.model.Stop;
import ch.bernmobil.netex.application.masterdata.model.StopPoint;

public class BernmobilStopFetcher {

	private static final Logger logger = LoggerFactory.getLogger(BernmobilStopFetcher.class);

	private final String url;
	private final int stopsPerRequest;
	private final ObjectMapper mapper;

	public BernmobilStopFetcher(String url, int stopsPerRequest) {
		this.mapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
		this.url = url;
		this.stopsPerRequest = stopsPerRequest;
	}

	public List<Stop> fetchStops() throws IOException, URISyntaxException {
		final List<Stop> result = new ArrayList<>();
		String version = null;
		for (int offset = 0; true; offset += stopsPerRequest) {
			final String json = fetchJson(offset, stopsPerRequest, version);
			if (json == null) {
				break;
			}

			final List<BernmobilStop> stops = mapJsonToObjects(json);
			if (stops.isEmpty()) {
				break;
			}

			if (version == null) {
				version = stops.get(0).version;
			}

			result.addAll(stops.stream().map(this::convertStop).filter(Objects::nonNull).collect(Collectors.toList()));

			logger.info("fetched a total of {} stops", result.size());
		}
		return result;
	}

	String fetchJson(int offset, int limit, String version) throws IOException, URISyntaxException {
		if (url == null || url.isEmpty()) {
			logger.info("url is missing, fetching stops is disabled");
			return null;
		}

		String urlWithParams = url;
		urlWithParams += "?offset=" + offset;
		urlWithParams += "&limit=" + limit;
		if (version != null) {
			urlWithParams += "&version=" + version;
		}

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

	private List<BernmobilStop> mapJsonToObjects(String json) throws IOException {
		final MappingIterator<BernmobilStop> it = mapper.readerFor(BernmobilStop.class).readValues(json);
		return it.readAll();
	}

	private Stop convertStop(BernmobilStop stop) {
		final Stop result = new Stop();
		result.setSloid(stop.sloid);
		result.setDidok(stop.didokNumberFull != null ? stop.didokNumberFull.toString() : null);

		if (stop.stopPoints != null) {
			result.setStopPoints(stop.stopPoints.stream().map(stopPoint -> convertStopPoint(stopPoint, result)).filter(Objects::nonNull)
					.collect(Collectors.toList()));
		}

		return result;
	}

	private StopPoint convertStopPoint(BernmobilStopPoint stopPoint, Stop stop) {
		final StopPoint result = new StopPoint();
		result.setSloid(stopPoint.sloid);
		result.setDidok(stopPoint.didokNumberFull);
		return result;
	}

	private static class BernmobilStop {
		public String sloid;
		public Integer didokNumberFull;
		public List<BernmobilStopPoint> stopPoints = new ArrayList<>();
		public String version;
	}

	private static class BernmobilStopPoint {
		public String sloid;
		public String didokNumberFull;
	}
}
