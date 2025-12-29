package ch.bernmobil.netex.api.controller;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.bernmobil.netex.api.service.AdminService;
import ch.bernmobil.netex.persistence.dom.ImportVersion;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(value = "/admin/v1", produces = "application/json")
public class AdminControllerV1 {

	private static final Logger logger = LoggerFactory.getLogger(AdminControllerV1.class);

	private final AdminService adminService;

	public AdminControllerV1(AdminService adminService) {
		this.adminService = adminService;
	}

	@Operation(description = "Returns all timetables that have at least one import version in the database.")
	@GetMapping("/importVersions/timetables")
	public ResponseEntity<List<String>> getTimetables() {
		return ResponseEntity.ok(adminService.getTimetables());
	}

	@Operation(description = "Returns all import versions in the database for a specific timetable.")
	@GetMapping("/importVersions/timetables/{timetable}")
	public ResponseEntity<List<String>> getVersions(String timetable) {
		return ResponseEntity.ok(adminService.getVersions(timetable));
	}

	@Operation(description = "Returns a specific import version.")
	@GetMapping("/importVersions/timetables/{timetable}/{version}")
	public ResponseEntity<ImportVersion> getVersion(String timetable, String version) {
		return adminService.getVersion(timetable, version).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@Operation(
			description = "Modifies the 'force' flag of a specific import version. Also sets the 'force' flag of all other import versions of the same timetable to 'false'.")
	@PutMapping("/importVersions/timetables/{timetable}/{version}/force")
	public ResponseEntity<Void> forceVersion(String timetable, String version, boolean force) {
		try {
			logger.info("set 'force' flag of version {} of timetable {} to {}", version, timetable, force);
			adminService.forceVersion(timetable, version, force);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			logger.error("failed to update 'force' flag", e);
			return ResponseEntity.notFound().build();
		}
	}

	@Operation(
			description = "Modifies the 'keep' flag of a specific import version. Also sets the 'keep' flag of all other import versions of the same timetable to 'false'.")
	@PutMapping("/importVersions/timetables/{timetable}/{version}/keep")
	public ResponseEntity<Void> keepVersion(String timetable, String version, boolean keep) {
		try {
			logger.info("set 'keep' flag of version {} of timetable {} to {}", version, timetable, keep);
			adminService.keepVersion(timetable, version, keep);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			logger.error("failed to update 'keep' flag", e);
			return ResponseEntity.notFound().build();
		}
	}

	@Operation(description = "Returns the active import version for each timetable.")
	@GetMapping("/importVersions/active")
	public ResponseEntity<Collection<ImportVersion>> getActiveVersions() {
		return ResponseEntity.ok(adminService.getActiveVersions());
	}
}
