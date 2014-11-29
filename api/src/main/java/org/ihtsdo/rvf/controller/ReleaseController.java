package org.ihtsdo.rvf.controller;

import org.ihtsdo.rvf.execution.service.ReleaseDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * A controller that handles API calls for uploading and checking status of previously published releases.
 * Note: The GET methods in this controller do not allow a release file to be downloaded - instead it only tells you if
 * the release is already known to RVF.
 */
@Controller
@RequestMapping("/releases")
public class ReleaseController {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseController.class);
    @Autowired
    private ReleaseDataManager releaseDataManager;

    @RequestMapping(value = "{version}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity uploadRelease(@RequestParam(value = "file") MultipartFile file,
                                 @PathVariable String version,
                                 @RequestParam(value = "overWriteExisting", required = false) boolean overWriteExisting,
                                 @RequestParam(value = "purgeExistingDatabase") boolean purgeExistingDatabase) {
        try {
            boolean result = releaseDataManager.uploadPublishedReleaseData(file.getInputStream(), version, overWriteExisting, purgeExistingDatabase);
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
        catch (IOException e) {
            logger.warn("Error getting input stream from upload. Nested exception is : \n" + e.fillInStackTrace());
            return new ResponseEntity<>("Error getting input stream from upload. Nested exception is : " + e.fillInStackTrace(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "{version}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getRelease(@PathVariable String version) {

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
    public java.util.Set<String> getAllKnownReleases() {

        return releaseDataManager.getAllKnownReleases();
    }
}
