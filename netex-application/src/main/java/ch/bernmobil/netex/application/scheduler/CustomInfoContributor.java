package ch.bernmobil.netex.application.scheduler;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;

@Component
public class CustomInfoContributor implements InfoContributor {

	private final ImportVersionRepository importVersionRepository;

	public CustomInfoContributor(ImportVersionRepository importVersionRepository) {
		this.importVersionRepository = importVersionRepository;
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("activeVersions", importVersionRepository.getActiveImportVersions());
		builder.withDetail("jvm", Map.of("version", System.getProperty("java.version"), "vendor", System.getProperty("java.vendor")));
	}
}
