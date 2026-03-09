package ch.bernmobil.netex.api.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository.Order;
import ch.bernmobil.netex.persistence.dom.ImportVersion;

@Service
public class AdminService {

	private final ImportVersionRepository repository;

	public AdminService(ImportVersionRepository repository) {
		this.repository = repository;
	}

	public List<String> getTimetables() {
		return repository.getAllImportVersions().stream().map(iv -> iv.timetable).distinct().toList();
	}

	public List<String> getVersions(String timetable) {
		return repository.getImportVersions(timetable, Order.NEWEST_FIRST).stream().map(iv -> iv.version).toList();
	}

	public Optional<ImportVersion> getVersion(String timetable, String version) {
		return repository.getImportVersion(timetable, version);
	}

	public void forceVersion(String timetable, String version, boolean force) {
		final List<ImportVersion> importVersions = repository.getImportVersions(timetable, Order.NEWEST_FIRST);
		boolean foundVersion = false;
		for (final ImportVersion importVersion : importVersions) {
			if (importVersion.version.equals(version)) {
				importVersion.force = force;
				foundVersion = true;
			} else {
				importVersion.force = false;
			}
		}
		if (foundVersion) {
			importVersions.forEach(repository::insertOrUpdate);
		} else {
			throw new IllegalArgumentException("could not find version " + version + " for timetable " + timetable);
		}
	}

	public void keepVersion(String timetable, String version, boolean keep) {
		final Optional<ImportVersion> importVersion = repository.getImportVersion(timetable, version);
		importVersion.ifPresentOrElse(iv -> {
			iv.keep = keep;
			repository.insertOrUpdate(iv);
		}, () -> {
			throw new IllegalArgumentException("could not find version " + version + " for timetable " + timetable);
		});
	}

	public Collection<ImportVersion> getActiveVersions() {
		return repository.getActiveImportVersions();
	}
}
