package org.ihtsdo.release.assertion.setup;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.project.MavenProject;
import org.ihtsdo.release.assertion.ResourceProvider;
import org.ihtsdo.release.assertion.ResourceProviderException;
import org.ihtsdo.release.assertion.WorkingDirectoryResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InputFileResourceProvider implements ResourceProvider {

	private final MavenProject project;
	private final ArtifactRepository artifactRepository;
	private File workingDirectory;

	private static final Logger LOGGER = LoggerFactory.getLogger(InputFileResourceProvider.class);

	public InputFileResourceProvider(MavenProject project, ArtifactRepository artifactRepository,
									 WorkingDirectoryResourceProvider workingDirectoryResourceProvider) {
		this.project = project;
		this.artifactRepository = artifactRepository;

		// todo: would be nicer to use an annotated constructor parameter to get this
		workingDirectory = workingDirectoryResourceProvider.getWorkingDirectoryForClass(getClass());
	}

	@Override
	public void init() throws ResourceProviderException {
		Set<Artifact> dependencies = project.getDependencyArtifacts();

		CollectionUtils.filter(dependencies, new Predicate<Artifact>() {
			@Override
			public boolean evaluate(Artifact artifact) {
				return "rf2".equals(artifact.getClassifier());
			}
		});

		String basedir = artifactRepository.getBasedir();
		ArtifactRepositoryLayout layout = artifactRepository.getLayout();

		for (Artifact dependency : dependencies) {
			String path = layout.pathOf(dependency);
			File rf2File = new File(basedir, path);
			LOGGER.info("Extracting RF2 input files {}", rf2File.getAbsolutePath());
			try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(rf2File))) {
				ZipEntry entry;
				while((entry = zipInputStream.getNextEntry()) != null) {
					String name = entry.getName();
					File file = new File(workingDirectory, name);
					if (entry.isDirectory()) {
						if (!file.isDirectory()) {
							if (!file.mkdirs()) {
								throw new ResourceProviderException("Failed to create directory " + file.getAbsoluteFile());
							}
						}
					} else {
						LOGGER.info("Extracting entry {}", name);
						Files.copy(zipInputStream, file.toPath());
					}
				}
			} catch (IOException e) {
				throw new ResourceProviderException("Failed to extract input file archive " + rf2File.getAbsolutePath(), e);
			}

		}
	}

	public File getRF2FilesDirectory() {
		return workingDirectory;
	}

}
