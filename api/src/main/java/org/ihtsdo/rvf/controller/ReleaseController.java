package org.ihtsdo.rvf.controller;

import java.io.IOException;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * A controller that handles API calls for uploading and checking status of previously published releases.
 * Note: The GET methods in this controller do not allow a release file to be downloaded - instead it only tells you if
 * the release is already known to RVF.
 */
@Controller
@RequestMapping("/releases")
@Api(value = "Releases")
@ApiIgnore //this is being marked as ignore as these services are already provided by SRS
public class ReleaseController {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseController.class);
    @Autowired
    private ReleaseDataManager releaseDataManager;

    @RequestMapping(value = "{product}/{version}", method = RequestMethod.POST)
    @ResponseBody
	@ApiOperation( value = "TBD", notes = "?" )
    public ResponseEntity uploadRelease(@RequestParam(value = "file") final MultipartFile file,
    							 @PathVariable final String product,
                                 @PathVariable final String version) {
        try {
            final boolean result = releaseDataManager.uploadPublishedReleaseData(file.getInputStream(), file.getOriginalFilename(), product, version);
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        catch (final IOException |BusinessServiceException e) {
            logger.warn("Error getting input stream from upload. Nested exception is : \n" + e.fillInStackTrace());
            return new ResponseEntity<>("Error getting input stream from upload. Nested exception is : " + e.fillInStackTrace(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "{version}", method = RequestMethod.GET)
    @ResponseBody
	@ApiOperation( value = "TBD", notes = "?" )
    public ResponseEntity getRelease(@PathVariable final String version) {

        if(releaseDataManager.isKnownRelease(version)){
            return new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
        }
        else{
            return new ResponseEntity<>(Boolean.TRUE, HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
	@ApiOperation( value = "TBD", notes = "?" )
    public java.util.Set<String> getAllKnownReleases() {

        return releaseDataManager.getAllKnownReleases();
    }
}
