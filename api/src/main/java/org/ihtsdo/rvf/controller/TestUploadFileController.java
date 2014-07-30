package org.ihtsdo.rvf.controller;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.rvf.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
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
        // must be a zip
        if (filename.endsWith(".zip")) {
            return uploadPostTestPackage(file, response);

        } else if (filename.endsWith(".txt")) {
            return uploadPreTestPackage(file, response);
        } else {
            throw new IllegalArgumentException("File should be pre or post and either a .txt or .zip is expected");
        }
    }

    @RequestMapping(value = "/test-post", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity uploadPostTestPackage(@RequestParam(value = "file") MultipartFile file, HttpServletResponse response) throws IOException {
        // load the filename
        String filename = file.getOriginalFilename();

        final File tempFile = File.createTempFile(filename, ".zip");
        tempFile.deleteOnExit();
        if (!filename.endsWith(".zip")) {
            throw new IllegalArgumentException("Post condition test package has to be zipped up");
        }

        // set up the response in order to strean directly to the response
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"report_" + filename + "_" + new Date() + "\"");
        PrintWriter writer = response.getWriter();

        // must be a zip
        copyUploadToDisk(file, tempFile);
        ResourceManager resourceManager = new ZipFileResourceProvider(tempFile);

        TestReportable report = validationRunner.execute(ResponseType.CSV, resourceManager, writer);

        // store the report to disk for now with a timestamp
        if (report.getNumErrors() > 0) {
            LOGGER.error("No Errors expected but got " + report.getNumErrors() + " errors");
        }

        return null;
    }

    @RequestMapping(value = "/test-pre", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity uploadPreTestPackage(@RequestParam(value = "file") MultipartFile file, HttpServletResponse response) throws IOException {
        // load the filename
        String filename = file.getOriginalFilename();

        if(!filename.startsWith("rel")) {
            LOGGER.error("Not a valid pre condition file " + filename);
            return null;
        }

        final File tempFile = File.createTempFile(filename, ".txt");
        tempFile.deleteOnExit();
        if (!filename.endsWith(".txt")) {
            throw new IllegalArgumentException("Pre condition file should always be a .txt file");
        }

        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"report_" + filename + "_" + new Date() + "\"");
        PrintWriter writer = response.getWriter();

        copyUploadToDisk(file, tempFile);
        ResourceManager resourceManager = new TextFileResourceProvider(tempFile, filename);
        TestReportable report = validationRunner.execute(ResponseType.CSV, resourceManager, writer);

        // store the report to disk for now with a timestamp
        if (report.getNumErrors() > 0) {
            LOGGER.error("No Errors expected but got " + report.getNumErrors() + " errors");
        }


        return null;
    }

    private void copyUploadToDisk(MultipartFile file, File tempFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            InputStream inputStream = file.getInputStream();
            IOUtils.copy(inputStream, out);
        }
    }
}
