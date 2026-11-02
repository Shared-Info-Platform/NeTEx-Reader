package ch.bernmobil.netex.application.masterdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.application.masterdata.model.Line;

public class LineRepository {

	private static final Logger logger = LoggerFactory.getLogger(LineRepository.class);

	private LineStorage lineStorage = new LineStorage();

	public void setLineStorage(LineStorage lineStorage) {
		this.lineStorage = lineStorage;
	}

	public List<Line> getLinesForOrganisationNumberAndText(String organisationNumber, String text) {
		return lineStorage.linesByOrganisationNumberAndLineText.getOrDefault(organisationNumber, Collections.emptyMap()).getOrDefault(text, Collections.emptyList());
	}

	public static class LineStorage {
		private final Map<String, Map<String, List<Line>>> linesByOrganisationNumberAndLineText = new HashMap<>();

		public void addLine(Line line) {
			final String organisationNumber = line.getOrganisationNumber();
			final String lineText = line.getText();
			final String regionNumber = line.getRegionNumber();

			final Map<String, List<Line>> map = linesByOrganisationNumberAndLineText.computeIfAbsent(organisationNumber, key -> new HashMap<>());
			final List<Line> list = map.computeIfAbsent(lineText, key -> new ArrayList<>());
			list.add(line);
			if (list.stream().filter(l -> Objects.equals(l.getRegionNumber(), regionNumber)).count() > 1) {
				logger.warn("duplicate line for operator {}, line text {}, and region code {}", organisationNumber, lineText, regionNumber);
			}
		}
	}
}
