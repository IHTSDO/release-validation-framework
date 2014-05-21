import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.ResourceProviderFactoryException;
import org.ihtsdo.release.assertion.setup.InputFileResourceProvider;

import java.io.File;

@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class ColumnPatternTestMojo extends AbstractMojo {

	@Component
	private ResourceProviderFactory resourceProviderFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			InputFileResourceProvider resourceProvider = resourceProviderFactory.getResourceProvider(InputFileResourceProvider.class);
			File rf2FilesDirectory = resourceProvider.getRF2FilesDirectory();
			// todo: test files
		} catch (ResourceProviderFactoryException e) {
			throw new MojoExecutionException("Failed to get InputFileResourceProvider", e);
		}

	}

}
