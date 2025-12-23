package ch.bernmobil.netex.application.scheduler;

import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.importer.ImporterProperties;
import ch.bernmobil.netex.persistence.export.NetexRepository;

public class ImporterFactory {

	public Importer createImporter(ImporterProperties properties, NetexRepository netexRepository) {
		return new Importer(properties, netexRepository);
	}
}
