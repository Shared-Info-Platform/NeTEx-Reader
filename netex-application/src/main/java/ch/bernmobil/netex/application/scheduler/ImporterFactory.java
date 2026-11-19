package ch.bernmobil.netex.application.scheduler;

import ch.bernmobil.netex.application.masterdata.LineRepository;
import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.importer.ImporterProperties;
import ch.bernmobil.netex.persistence.export.NetexRepository;

public class ImporterFactory {

	private final LineRepository lineRepository;

	public ImporterFactory(LineRepository lineRepository) {
		this.lineRepository = lineRepository;
	}

	public Importer createImporter(ImporterProperties properties, NetexRepository netexRepository) {
		return new Importer(properties, netexRepository, lineRepository);
	}
}
