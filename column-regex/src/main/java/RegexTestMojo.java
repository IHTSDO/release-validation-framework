import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.ihtsdo.release.assertion.AssertionResourceFactory;

@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class RegexTestMojo extends AbstractMojo {

	@Component
	private AssertionResourceFactory assertionResourceFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("RegexTestMojo!!");
		getLog().info("AssertionResourceFactory = " + assertionResourceFactory);
		getLog().info("thing = " + assertionResourceFactory.getResource("thing"));
		getLog().info("thing = " + assertionResourceFactory.getResource("thing"));
	}

}
