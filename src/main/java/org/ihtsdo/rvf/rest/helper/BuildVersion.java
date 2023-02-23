package org.ihtsdo.rvf.rest.helper;

public class BuildVersion {

    private final String version;
    private final String time;

    public BuildVersion(String version, String time) {
        this.version = version;
        this.time = time;
    }

    public String getVersion() {
        return version;
    }

    public String getTime() {
        return time;
    }
}