package org.ihtsdo.rvf.controller;

import java.io.IOException;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


/**
 * A controller that handles API calls for uploading and checking status of
 * previously published releases. Note: The GET methods in this controller do
 * not allow a release file to be downloaded - instead it only tells you if the
 * release is already known to RVF.
 */
@RestController
@RequestMapping("/releases")
@Api(tags = "Published releases", description = "-")
public class ReleaseController {

	private static final Logger logger = LoggerFactory.getLogger(ReleaseController.class);
	@Autowired
	private ReleaseDataManager releaseDataManager;
	
	@RequestMapping(value = "{product}/{version}", method = RequestMethod.POST)
	@ResponseBody
	@ApiOperation(value = "Upload a published release version", notes = "Uploads a published release for a given product.")
	public ResponseEntity uploadRelease(
			@ApiParam(value = "The published RF2 zip package") @RequestParam(value = "file") final MultipartFile file,
			@ApiParam(value = "The short product name e.g int for international RF2 release") @PathVariable final String product,
			@ApiParam(value = "The release date in yyyymmdd e.g 20170131") @PathVariable final String version) {
		try {
			final boolean result = releaseDataManager
					.uploadPublishedReleaseData(file.getInputStream(),
							file.getOriginalFilename(), product, version);
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
	@ApiOperation(value = "Check a given release is loaded already", notes = "Checks whether a version is loaded or not. The version format is rvf_{product}_{releaseDate} e.g rvf_int_20170131")
	public ResponseEntity getRelease(
			@ApiParam(value = "The version name e.g rvf_int_20170131") @PathVariable final String version) {
		if (releaseDataManager.isKnownRelease(version)) {
			return new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
		} else {
			return new ResponseEntity<>(Boolean.TRUE, HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get all versions that are loaded in the RVF database", 
	notes = "Gets all versions that are loaded in the RVF database. Published versions are loaded in the format of rvf_{product}_{releaseDate} e.g rvf_int_20170131.")
	public java.util.Set<String> getAllKnownReleases() {
		return releaseDataManager.getAllKnownReleases();
	}
}
