package org.ihtsdo.release.assertion;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

@Mojo(name = "init", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class AssertionResourceFactoryMojo extends AbstractMojo {

	@Component
	private ResourceProviderFactory resourceProviderFactory;

	@Component(role = MavenProject.class)
	private MavenProject project;

	@Parameter(defaultValue = "${localRepository}", required = true)
	private ArtifactRepository localRepository;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ResourceProviderFactoryImpl factory = (ResourceProviderFactoryImpl) resourceProviderFactory;
		factory.init(project, localRepository);
	}

}
