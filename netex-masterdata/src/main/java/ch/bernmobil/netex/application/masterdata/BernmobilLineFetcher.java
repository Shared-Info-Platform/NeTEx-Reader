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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import ch.bernmobil.netex.application.masterdata.model.Line;

public class BernmobilLineFetcher {

	private static final Logger logger = LoggerFactory.getLogger(BernmobilLineFetcher.class);

	private static final Pattern PAG_LINE_ID_PATTERN = Pattern.compile("85\\:801\\:(\\d+)");

	private final String url;
	private final int linesPerRequest;
	private final ObjectMapper mapper;

	public BernmobilLineFetcher(String url, int linesPerRequest) {
		this.mapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
		this.url = url;
		this.linesPerRequest = linesPerRequest;
	}

	public List<Line> fetchLines() throws IOException, URISyntaxException {
		final List<Line> result = new ArrayList<>();
		for (int pageNumber = 0; true; ++pageNumber) {
			final String json = fetchJson(pageNumber, linesPerRequest);
			if (json == null) {
				break;
			}

			final BernmobilPage page = mapJsonToObject(json);
			if (page == null || page.content == null || page.content.isEmpty()) {
				break;
			}

			result.addAll(page.content.stream().map(this::convertLine).filter(Objects::nonNull).collect(Collectors.toList()));

			logger.info("fetched a total of {} lines", result.size());
		}
		return result;
	}

	String fetchJson(int pageNumber, int limit) throws IOException, URISyntaxException {
		if (url == null || url.isEmpty()) {
			logger.info("url is missing, fetching lines is disabled");
			return null;
		}

		String urlWithParams = url;
		urlWithParams += "?page=" + pageNumber;
		urlWithParams += "&size=" + limit;
		urlWithParams += "&withReferencedEntities=true&valid=true&isSubline=false";

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

	private Line convertLine(BernmobilLine line) {
		final Line result = new Line();
		result.setLineId(line.linienId);
		result.setSlnid(line.slnid);
		result.setText(line.number);
		if (line.operator != null && line.operator.organisationNumber != null) {
			result.setOrganisationNumber(line.operator.organisationNumber.toString());
		}

		if (line.linienId != null) {
			final Matcher matcher = PAG_LINE_ID_PATTERN.matcher(line.linienId);
			if (matcher.matches()) {
				result.setRegionNumber(matcher.group(1));
			}
		}
		return result;
	}

	private static class BernmobilPage {
		public List<BernmobilLine> content;
	}

	private static class BernmobilLine {
		public String linienId;
		public String number;
		public String slnid;
		public BernmobilOperator operator;
	}

	private static class BernmobilOperator {
		public Integer organisationNumber;
	}
}
