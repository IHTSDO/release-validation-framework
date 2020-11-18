package org.ihtsdo.rvf.validation;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.rvf.validation.log.ValidationLog;
import org.ihtsdo.rvf.validation.model.ManifestFile;
import org.ihtsdo.rvf.validation.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ihtsdo.rvf.validation.model.manifest.*;

public class ManifestRefsetTester {

    private static final String REFSET_STRUCTURE_TEST = "RefsetStructureTest";
    private static final String REFSET_DESCRIPTOR_SNAPSHOT_PATTERN = "RefsetDescriptorSnapshot";
    private static final String MANIFEST = "manifest.xml";
    private static final String UTF_8 = "UTF-8";
    private static final String RF2_LINE_SEPARATOR = "\r\n";
    public static final String LINE_ENDING = RF2_LINE_SEPARATOR;
    private final ValidationLog validationLog;
    private final ResourceProvider resourceManager;
    private final ManifestFile manifestFile;
    private final TestReportable report;
    private Map<String, Set<String>> refsetMap = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestRefsetTester.class);
    private Date startDate;

    public ManifestRefsetTester(ValidationLog validationLog, ResourceProvider resourceManager, ManifestFile manifestFile, TestReportable report) {
        this.validationLog = validationLog;
        this.resourceManager = resourceManager;
        this.manifestFile = manifestFile;
        this.report = report;
        try {
            createRefsetMap();
        } catch (IOException | JAXBException | BusinessServiceException e) {
            this.validationLog.assertionError("Error", e.getMessage());
        }
    }

    public void runTests() {
        startDate = new Date();
        testRefsetFilesWithManifest();
    }


    private void createRefsetMap() throws FileNotFoundException, JAXBException, UnsupportedEncodingException, BusinessServiceException {
        InputStream manifestInputStream = new FileInputStream(manifestFile.getFile());

        //Load the manifest file xml into a java object hierarchy
        JAXBContext jc = JAXBContext.newInstance("org.ihtsdo.rvf.validation.model.manifest");
        Unmarshaller um = jc.createUnmarshaller();
        ListingType manifestListing = um.unmarshal(new StreamSource(new InputStreamReader(manifestInputStream, "UTF-8")), ListingType.class).getValue();

        if (manifestListing.getFolder() == null) {
            throw new BusinessServiceException("Failed to recover root folder from manifest.  Ensure the root element is named 'listing' "
                    + "and it has a namespace of xmlns=\"http://release.ihtsdo.org/manifest/1.0.0\" ");
        }
        getRefsetsFromManifest(manifestListing.getFolder());
    }

    private void getRefsetsFromManifest(FolderType folder) {
        if(folder != null) {
            if(folder.getFile() != null) {
                for (FileType file : folder.getFile()) {
                    if(file.getContainsReferenceSets() != null) {
                        for (RefsetType refset : file.getContainsReferenceSets().getRefset()) {
                            String fileName = file.getName().replace("Delta","###");
                            if (!refsetMap.containsKey(fileName)) {
                                refsetMap.put(fileName, new HashSet<>());
                            }
                            refsetMap.get(fileName).add(refset.getId().toString());
                        }
                    }
                }
            }
            if(folder.getFolder() != null) {
                for (FolderType subFolder : folder.getFolder()) {
                    getRefsetsFromManifest(subFolder);
                }
            }
        }

    }

    
    public void testRefsetFilesWithManifest(){
        List<String> fileNames = resourceManager.getFileNames();
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Boolean>> futures = new ArrayList<>();
        for (final String fileName : fileNames) {
            if (!fileName.endsWith(".txt") && !fileName.contains("Refset_")) {
                continue;
            }
            Future<Boolean> task = executorService.submit(new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    return runTestForRefsetFileWithManifest(fileName);
                }
            });
            futures.add(task);
        }
        for (Future<Boolean> task : futures) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Task failed when structure testing due to:", e);
                validationLog.executionError("Error", "Failed to check file due to:" + e.fillInStackTrace());
            }
        }
    }

    private boolean runTestForRefsetFileWithManifest(String fileName) {
        String keyName = fileName.replace("Delta","###")
                .replace("Snapshot","###")
                .replace("Full","###");
        Set<String> refsetForFile = refsetMap.get(keyName);
        String expectedRefsetIds = StringUtils.join(refsetForFile,",");
        if(refsetForFile != null && !refsetForFile.isEmpty()) {
            try {
                Reader reader = resourceManager.getReader(fileName, Charset.forName(UTF_8));
                LineIterator lineIterator = IOUtils.lineIterator(reader);
                //Only run test if file is not empty
                if(lineIterator.hasNext()) {
                    //Skip header line
                    lineIterator.next();
                    long lineNum = 1;
                    while (lineIterator.hasNext()) {
                        lineNum++;
                        String line = lineIterator.next();
                        String[] columns = line.split("\t");
                        //Only test if the records has column active = 1
                        if("1".equals(columns[2])) {
                            String refsetId = columns[4];
                            if(!refsetForFile.contains(refsetId)) {
                                String error = "RefsetId " + refsetId + " at line " + lineNum + " is not specified for " + fileName + " in manifest.xml";
                                String expected = "RefsetId " + expectedRefsetIds;
                                report.addError("",startDate,fileName,resourceManager.getFilePath(),"refsetId", REFSET_STRUCTURE_TEST,expectedRefsetIds,error,expected,lineNum);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}

