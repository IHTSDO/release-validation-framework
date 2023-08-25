package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.rvf.rest.helper.BuildVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Version")
public class VersionController {
	@Autowired(required = false)
	private BuildProperties buildProperties;

	@Operation(summary = "Software build version and timestamp.")
	@GetMapping(value = "/version", produces = "application/json")
	public BuildVersion getBuildInformation() {
		if (buildProperties == null) {
			throw new IllegalStateException("Build properties are not present.");
		}
		return new BuildVersion(buildProperties.getVersion(), buildProperties.getTime().toString());
	}
}
