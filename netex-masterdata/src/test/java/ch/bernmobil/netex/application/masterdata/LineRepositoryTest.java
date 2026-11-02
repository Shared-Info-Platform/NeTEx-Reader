package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.bernmobil.netex.application.masterdata.LineRepository.LineStorage;
import ch.bernmobil.netex.application.masterdata.model.Line;

public class LineRepositoryTest {

	private LineRepository lineRepository;

	@BeforeEach
	public void setup() {
		lineRepository = new LineRepository();
	}

	@Test
	public void testCanGetLineByOperatorAndText() {
		final Line line1 = new Line();
		line1.setText("1");
		line1.setOrganisationNumber("801");
		final Line line2 = new Line();
		line2.setText("2");
		line2.setOrganisationNumber("801");
		final Line line3 = new Line();
		line3.setText("1");
		line3.setOrganisationNumber("820");

		setStorageWithLines(line1, line2, line3);

		assertThat(lineRepository.getLinesForOrganisationNumberAndText("801", "1")).containsExactly(line1);
		assertThat(lineRepository.getLinesForOrganisationNumberAndText("801", "2")).containsExactly(line2);
		assertThat(lineRepository.getLinesForOrganisationNumberAndText("801", "3")).isEmpty();
		assertThat(lineRepository.getLinesForOrganisationNumberAndText("820", "1")).containsExactly(line3);
		assertThat(lineRepository.getLinesForOrganisationNumberAndText("840", "1")).isEmpty();
	}

	@Test
	public void testReturnsAllLinesOnDuplicateText() {
		final Line line1 = new Line();
		line1.setText("1");
		line1.setOrganisationNumber("801");
		final Line line2 = new Line();
		line2.setText("1");
		line2.setOrganisationNumber("801");

		setStorageWithLines(line1, line2);

		assertThat(lineRepository.getLinesForOrganisationNumberAndText("801", "1")).containsExactly(line1, line2);
	}

	@Test
	public void testReturnsAllWithDifferentRegionNumber() {
		final Line line1 = new Line();
		line1.setText("1");
		line1.setOrganisationNumber("801");
		line1.setRegionNumber("1");
		final Line line2 = new Line();
		line2.setText("1");
		line2.setOrganisationNumber("801");
		line2.setRegionNumber("2");

		setStorageWithLines(line1, line2);

		assertThat(lineRepository.getLinesForOrganisationNumberAndText("801", "1")).containsExactly(line1, line2);
	}

	private void setStorageWithLines(Line ... lines) {
		final LineStorage storage = new LineStorage();
		for (final Line line : lines) {
			storage.addLine(line);
		}
		lineRepository.setLineStorage(storage);
	}
}
