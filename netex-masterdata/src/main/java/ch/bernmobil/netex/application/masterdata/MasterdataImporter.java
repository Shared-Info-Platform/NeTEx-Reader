package ch.bernmobil.netex.application.masterdata;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ch.bernmobil.netex.application.masterdata.LineRepository.LineStorage;
import ch.bernmobil.netex.application.masterdata.OperatorRepository.OperatorStorage;
import ch.bernmobil.netex.application.masterdata.StopRepository.StopStorage;
import ch.bernmobil.netex.application.masterdata.model.Line;
import ch.bernmobil.netex.application.masterdata.model.Operator;
import ch.bernmobil.netex.application.masterdata.model.Stop;
import jakarta.annotation.PostConstruct;

@Service
public class MasterdataImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MasterdataImporter.class);

	private final BernmobilStopFetcher stopFetcher;
	private final BernmobilLineFetcher lineFetcher;
	private final BernmobilOperatorFetcher operatorFetcher;
	private final StopRepository stopRepository;
	private final LineRepository lineRepository;
	private final OperatorRepository operatorRepository;

	public MasterdataImporter(BernmobilStopFetcher stopFetcher, BernmobilLineFetcher lineFetcher, BernmobilOperatorFetcher operatorFetcher,
			StopRepository stopRepository, LineRepository lineRepository, OperatorRepository operatorRepository) {
		this.stopFetcher = stopFetcher;
		this.lineFetcher = lineFetcher;
		this.operatorFetcher = operatorFetcher;
		this.stopRepository = stopRepository;
		this.lineRepository = lineRepository;
		this.operatorRepository = operatorRepository;
	}

	@PostConstruct
	@Scheduled(cron = "${masterdata.importCronExpression:0 0 23 * * *}")
	public void importMasterdataIntoRepository() {
		importStops();
		importLines();
		importOperators();
	}

	private void importStops() {
		try {
			LOGGER.info("import stops");
			final List<Stop> stops = stopFetcher.fetchStops();

			final StopStorage stopStorage = new StopStorage();
			stops.forEach(stopStorage::addStop);

			stopRepository.setStopStorage(stopStorage); // replace all stops in one atomic step to avoid problems with parallel access
		} catch (IOException | URISyntaxException | RuntimeException e) {
			LOGGER.error("failed to import stops", e);
		}
	}

	private void importLines() {
		try {
			LOGGER.info("import lines");
			final List<Line> lines = lineFetcher.fetchLines();

			final LineStorage lineStorage = new LineStorage();
			lines.forEach(lineStorage::addLine);

			lineRepository.setLineStorage(lineStorage); // replace all lines in one atomic step to avoid problems with parallel access
		} catch (IOException | URISyntaxException | RuntimeException e) {
			LOGGER.error("failed to import lines", e);
		}
	}

	private void importOperators() {
		try {
			LOGGER.info("import operators");
			final List<Operator> operators = operatorFetcher.fetchOperators();

			final OperatorStorage operatorStorage = new OperatorStorage();
			operators.forEach(operatorStorage::addOperator);

			operatorRepository.setOperatorStorage(operatorStorage); // replace all operators in one atomic step to avoid problems with parallel access
		} catch (IOException | URISyntaxException | RuntimeException e) {
			LOGGER.error("failed to import operators", e);
		}
	}
}
