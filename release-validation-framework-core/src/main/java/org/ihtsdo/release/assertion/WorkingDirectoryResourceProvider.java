package org.ihtsdo.release.assertion;

import org.apache.maven.project.MavenProject;

import java.io.File;

public class WorkingDirectoryResourceProvider implements ResourceProvider {

	private final File targetDirectory;

	public WorkingDirectoryResourceProvider(MavenProject project) {
		File basedir = project.getBasedir();
		targetDirectory = new File(basedir, "target");
	}

	public File getWorkingDirectoryForClass(Class aClass) {
		File classWorkingDirectory = new File(targetDirectory, aClass.getCanonicalName());
		if (!classWorkingDirectory.isDirectory()) {
			if (!classWorkingDirectory.mkdirs()) {
				throw new RuntimeException("Failed to create directory " + classWorkingDirectory.getAbsolutePath());
			}
		}
		return classWorkingDirectory;
	}

	@Override
	public void init() {
	}

}
