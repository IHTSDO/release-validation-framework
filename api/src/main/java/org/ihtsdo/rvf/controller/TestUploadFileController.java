package org.ihtsdo.rvf.controller;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * The controller that handles uploaded files for the validation to run
 */
@Controller
public class TestUploadFileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUploadFileController.class);

    @Autowired
    private ValidationTestRunner validationRunner;

    @RequestMapping(value = "/test-file", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity uploadTestPackage(@RequestParam(value = "file") MultipartFile file, HttpServletResponse response) throws IOException {
        // load the filename
        String filename = file.getOriginalFilename();

		final File tempFile = File.createTempFile(filename, ".zip");
		tempFile.deleteOnExit();

		// must be a zip
        ResourceManager resourceManager;
        if (filename.endsWith(".zip")) {
			copyUploadToDisk(file, tempFile);
			resourceManager = new ZipFileResourceProvider(tempFile);
		} else if (filename.endsWith(".txt")) {
			copyUploadToDisk(file, tempFile);
			resourceManager = new TextFileResourceProvider(tempFile, filename);
		} else {
			throw new IllegalArgumentException("Only zip file accepted");
		}

		TestReport report = validationRunner.execute(ResponseType.CSV, resourceManager);

        // store the report to disk for now with a timestamp
        if(report.getErrorCount() > 0) {
            LOGGER.error("No Errors expected but got " + report.getErrorCount() + " errors");
            //return new ResponseEntity<>(report.getResult(), HttpStatus.EXPECTATION_FAILED);
        }

        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"report_" + filename + "_" + new Date() + "\"");
        response.getOutputStream().print(report.getResult());

//        return new ResponseEntity<>(report.getResult(), HttpStatus.OK);
        return null;
    }

	private void copyUploadToDisk(MultipartFile file, File tempFile) throws IOException {
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			InputStream inputStream = file.getInputStream();
			IOUtils.copy(inputStream, out);
		}
	}
}
