package au.csiro.datachecks.framework;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.codehaus.plexus.util.FileUtils;

/**
 * Traverses directory trees to locate files.
 */
public class FileScanner extends DirectoryWalker {

    private List<File> files = new ArrayList<>();
    
    public FileScanner() {
        super();        
        addDirectoryWalkListener(new DirectoryWalkListener() {
            
            public void directoryWalkStep(int percentage, File file) {}
            public void directoryWalkFinished() {}
            public void debug(String message) {}
            
            @SuppressWarnings("unchecked")
            public void directoryWalkStarting(File basedir) {
                try {
                    List<String> allFilenames = (List<String>) FileUtils.getFileNames(basedir, null, null, false);
                    for (String filename : allFilenames) {
                        files.add(new File(basedir, filename));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    
    /**
     * Get a list of all the files within a directory tree
     * 
     * @param rootDir
     * @return
     */
    public static List<File> getAllFiles(File rootDir) {
        FileScanner scanner = new FileScanner();
        scanner.setBaseDir(rootDir);
        try {
            scanner.scan();    
        } catch (IllegalStateException e) {
            throw new RuntimeException("Failed scanning directory '" + rootDir + "'", e);            
        }
        return scanner.files;
    }
    
}
