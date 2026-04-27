package ch.bernmobil.netex.application.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.model.ImportVersion;

@Component
public class CustomInfoContributor implements InfoContributor {

	private final ImportSchedulerProperties properties;
	private final ImportVersionRepository importVersionRepository;

	public CustomInfoContributor(ImportSchedulerProperties properties, ImportVersionRepository importVersionRepository) {
		this.properties = properties;
		this.importVersionRepository = importVersionRepository;
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("activeVersions", importVersionRepository.getActiveImportVersions());
		builder.withDetail("lastImports", getLastImports());
		builder.withDetail("jvm", Map.of("version", System.getProperty("java.version"), "vendor", System.getProperty("java.vendor")));
	}

	private List<ImportVersion> getLastImports() {
		return properties.getUriPerTimetable().keySet().stream()
				.map(importVersionRepository::getLastImportVersion)
				.map(optional -> optional.orElse(null))
				.filter(Objects::nonNull)
				.toList();
	}
}
