package ch.bernmobil.netex.application.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.bernmobil.netex.application.masterdata.model.Line;
import ch.bernmobil.netex.application.masterdata.model.Operator;
import ch.bernmobil.netex.application.masterdata.model.Stop;
import ch.bernmobil.netex.application.masterdata.model.StopPoint;

public class MasterdataImporterTest {

	private MasterdataImporter masterdataImporter;
	private BernmobilStopFetcher stopFetcher;
	private BernmobilLineFetcher lineFetcher;
	private BernmobilOperatorFetcher operatorFetcher;
	private StopRepository stopRepository;
	private LineRepository lineRepository;
	private OperatorRepository operatorRepository;

	@BeforeEach
	public void setup() {
		stopFetcher = Mockito.mock(BernmobilStopFetcher.class);
		lineFetcher = Mockito.mock(BernmobilLineFetcher.class);
		operatorFetcher = Mockito.mock(BernmobilOperatorFetcher.class);
		stopRepository = new StopRepository();
		lineRepository = new LineRepository();
		operatorRepository = new OperatorRepository();

		masterdataImporter = new MasterdataImporter(stopFetcher, lineFetcher, operatorFetcher, stopRepository, lineRepository, operatorRepository);
	}

	@Test
	public void testImportsStops() throws IOException, URISyntaxException {
		final Stop stop1 = new Stop();
		stop1.setDidok("8508450");
		stop1.setSloid("ch:1:sloid:8508450");

		final StopPoint stopPoint1 = new StopPoint();
		stopPoint1.setDidok("850845001");
		stopPoint1.setSloid("ch:1:sloid:8508450::1");
		stop1.getStopPoints().add(stopPoint1);
		final StopPoint stopPoint2 = new StopPoint();
		stopPoint2.setDidok("850845002");
		stopPoint2.setSloid("ch:1:sloid:8508450::2");
		stop1.getStopPoints().add(stopPoint2);

		final Stop stop2 = new Stop();
		stop2.setDidok("8505000");
		stop2.setSloid("ch:1:sloid:8505000");

		final StopPoint stopPoint3 = new StopPoint();
		stopPoint3.setDidok("850500001");
		stopPoint3.setSloid("ch:1:sloid:8505000::1");
		stop2.getStopPoints().add(stopPoint3);
		final StopPoint stopPoint4 = new StopPoint();
		stopPoint4.setDidok("850500002");
		stopPoint4.setSloid("ch:1:sloid:8505000::2");
		stop2.getStopPoints().add(stopPoint4);

		Mockito.when(stopFetcher.fetchStops()).thenReturn(List.of(stop1, stop2));

		masterdataImporter.importMasterdataIntoRepository();

		assertThat(stopRepository.getSloidForDidok("8508450")).isEqualTo("ch:1:sloid:8508450");
		assertThat(stopRepository.getSloidForDidok("8505000")).isEqualTo("ch:1:sloid:8505000");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8508450")).isEqualTo("8508450");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8505000")).isEqualTo("8505000");
		assertThat(stopRepository.getSloidForDidok("850845001")).isEqualTo("ch:1:sloid:8508450::1");
		assertThat(stopRepository.getSloidForDidok("850845002")).isEqualTo("ch:1:sloid:8508450::2");
		assertThat(stopRepository.getSloidForDidok("850500001")).isEqualTo("ch:1:sloid:8505000::1");
		assertThat(stopRepository.getSloidForDidok("850500002")).isEqualTo("ch:1:sloid:8505000::2");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8508450::1")).isEqualTo("850845001");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8508450::2")).isEqualTo("850845002");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8505000::1")).isEqualTo("850500001");
		assertThat(stopRepository.getDidokForSloid("ch:1:sloid:8505000::2")).isEqualTo("850500002");
	}

	@Test
	public void testImportsLines() throws IOException, URISyntaxException {
		final Line line1 = new Line();
		line1.setLineId("85:820:1");
		line1.setSlnid("ch:1:slnid:123");
		line1.setText("1");
		line1.setOrganisationNumber("820");

		final Line line2 = new Line();
		line2.setLineId("85:820:2");
		line2.setSlnid("ch:1:slnid:234");
		line2.setText("2");
		line2.setOrganisationNumber("820");

		Mockito.when(lineFetcher.fetchLines()).thenReturn(List.of(line1, line2));

		masterdataImporter.importMasterdataIntoRepository();

		assertThat(lineRepository.getLinesForOrganisationNumberAndText("820", "1")).containsExactly(line1);
		assertThat(lineRepository.getLinesForOrganisationNumberAndText("820", "2")).containsExactly(line2);
	}

	@Test
	public void testImportsOperators() throws IOException, URISyntaxException {
		final Operator operator1 = new Operator();
		operator1.setOperatorId("85:820");
		operator1.setOrganisationNumber("820");

		final Operator operator2 = new Operator();
		operator2.setOperatorId("85:801");
		operator2.setOrganisationNumber("801");

		Mockito.when(operatorFetcher.fetchOperators()).thenReturn(List.of(operator1, operator2));

		masterdataImporter.importMasterdataIntoRepository();

		assertThat(operatorRepository.getOperatorForOrganisationNumber("820")).isSameAs(operator1);
		assertThat(operatorRepository.getOperatorForOrganisationNumber("801")).isSameAs(operator2);
	}

	@Test
	public void testCatchesExceptionsFromStopFetcher() throws IOException, URISyntaxException {
		Mockito.when(stopFetcher.fetchStops()).thenThrow(RuntimeException.class);
		masterdataImporter.importMasterdataIntoRepository();
	}

	@Test
	public void testCatchesExceptionsFromLineFetcher() throws IOException, URISyntaxException {
		Mockito.when(lineFetcher.fetchLines()).thenThrow(RuntimeException.class);
		masterdataImporter.importMasterdataIntoRepository();
	}

	@Test
	public void testCatchesExceptionsFromOperatorFetcher() throws IOException, URISyntaxException {
		Mockito.when(operatorFetcher.fetchOperators()).thenThrow(RuntimeException.class);
		masterdataImporter.importMasterdataIntoRepository();
	}
}
