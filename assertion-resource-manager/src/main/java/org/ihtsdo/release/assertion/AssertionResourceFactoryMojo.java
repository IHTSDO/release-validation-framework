package org.ihtsdo.release.assertion;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "init", defaultPhase = LifecyclePhase.TEST)
public class AssertionResourceFactoryMojo extends AbstractMojo {

	@Component
	private AssertionResourceFactory assertionResourceFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("AssertionResourceFactoryMojo setup");
		getLog().info("assertionResourceFactory = " + assertionResourceFactory);
		getLog().info("thing = " + assertionResourceFactory.getResource("thing"));
		getLog().info("thing = " + assertionResourceFactory.getResource("thing"));

	}

}
