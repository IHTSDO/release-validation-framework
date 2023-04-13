package au.csiro.datachecks.framework;

import java.util.logging.Logger;

public class AutoTimer implements AutoCloseable {

    private long startTime = System.currentTimeMillis();
    private String name;
    private Logger log;

    public AutoTimer(String name) {
        this(name, Logger.getLogger(FileLoader.class.getName()));
    }

    public AutoTimer(String name, Logger logger) {
        this.name = name;
        this.log = logger;
        log.info("Autotimer commencing task: '" + name + "'");
    }

    @Override
    public void close() throws RuntimeException {
        long durationMs = System.currentTimeMillis() - startTime;
        int seconds = (int) ((durationMs / 1000) % 60);
        int minutes = (int) ((durationMs / 1000) / 60);
        log.info(String.format("Completed %s in %d min %d seconds.", name, minutes, seconds));
    }

}
