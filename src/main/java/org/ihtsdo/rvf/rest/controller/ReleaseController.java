package org.ihtsdo.rvf.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.core.service.ReleaseDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * A controller that handles API calls for uploading and checking status of
 * previously published releases. Note: The GET methods in this controller do
 * not allow a release file to be downloaded - instead it only tells you if the
 * release is already known to RVF.
 */
@RestController
@RequestMapping("/releases")
@Tag(name = "Published Releases")
public class ReleaseController {

	private static final Logger logger = LoggerFactory.getLogger(ReleaseController.class);
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	@RequestMapping(value = "{product}/{version}", method = RequestMethod.POST, consumes = "multipart/form-data")
	@ResponseBody
	@Operation(summary = "Upload a published release version", description = "Uploads a published release for a given product.")
	public ResponseEntity<?> uploadRelease(
			@Parameter(description = "The published RF2 zip package") @RequestParam(value = "file") final MultipartFile file,
			@Parameter(description = "The short product name e.g int for international RF2 release") @PathVariable final String product,
			@Parameter(description = "The release date in yyyymmdd e.g 20170131") @PathVariable final String version) {
		try {
			final boolean result = releaseDataManager.uploadPublishedReleaseData(file.getInputStream(), file.getOriginalFilename(), product, version);
			return new ResponseEntity<>(result, HttpStatus.OK);
		} catch (final IOException | BusinessServiceException e) {
			logger.warn("Error getting input stream from upload. Nested exception is : \n"
					+ e.fillInStackTrace());
			return new ResponseEntity<>(
					"Error getting input stream from upload. Nested exception is : "
							+ e.fillInStackTrace(), HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "{version}", method = RequestMethod.GET)
	@ResponseBody
	@Operation(summary = "Check a given release is loaded already", description = "Checks whether a version is loaded or not. The version format is rvf_{product}_{releaseDate} e.g rvf_int_20170131")
	public ResponseEntity<?> getRelease(
			@Parameter(description = "The version name e.g rvf_int_20170131") @PathVariable final String version) {
		if (releaseDataManager.isKnownRelease(version)) {
			return new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(Boolean.FALSE, HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Get all versions that are loaded in the RVF database",
	description = "Gets all versions that are loaded in the RVF database. Published versions are loaded in the format of rvf_{product}_{releaseDate} e.g rvf_int_20170131.")
	public java.util.Set<String> getAllKnownReleases() {
		return releaseDataManager.getAllKnownReleases();
	}
}
