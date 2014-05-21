package org.ihtsdo.release.assertion;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.ihtsdo.release.assertion._1_0.ColumnPatternTestConfiguration;
import org.ihtsdo.release.assertion.setup.InputFileResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Mojo(name = "test", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class ColumnPatternTestMojo extends AbstractMojo {

	@Component
	private ResourceProviderFactory resourceProviderFactory;
	private Map<String, Pattern> regexCache;

	private static final Logger LOGGER = LoggerFactory.getLogger(ColumnPatternTestMojo.class);

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			ColumnPatternTestConfiguration configuration = loadConfiguration();
			regexCache = testConfigurationByPrecompilingRegexPatterns(configuration);

			InputFileResourceProvider resourceProvider = resourceProviderFactory.getResourceProvider(InputFileResourceProvider.class);
			File rf2FilesDirectory = resourceProvider.getRF2FilesDirectory();
			// todo: test files
		} catch (ResourceProviderFactoryException e) {
			throw new MojoExecutionException("Failed to get InputFileResourceProvider", e);
		}
	}

	/**
	 * Loads configuration from XML file on the class path.
	 * @return ColumnPatternTestConfiguration
	 * @throws MojoExecutionException
	 */
	private ColumnPatternTestConfiguration loadConfiguration() throws MojoExecutionException {
		try {
			InputStream configurationStream = resourceProviderFactory.getConfigurationStream("column-pattern-configuration.xml");
			JAXBContext jaxbContext = JAXBContext.newInstance(ColumnPatternTestConfiguration.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			JAXBElement<ColumnPatternTestConfiguration> configurationJAXBElement = unmarshaller.unmarshal(new StreamSource(configurationStream), ColumnPatternTestConfiguration.class);
			return configurationJAXBElement.getValue();
		} catch (JAXBException e) {
			throw new MojoExecutionException("Failed to unmarshal XML configuration file.", e);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException("Could not find XML configuration file.");
		}
	}

	private Map<String, Pattern> testConfigurationByPrecompilingRegexPatterns(ColumnPatternTestConfiguration configuration) {
		Map<String, Pattern> regexCache = new HashMap<>();
		for (ColumnPatternTestConfiguration.File file : configuration.getFile()) {
			for (ColumnPatternTestConfiguration.File.Column column : file.getColumn()) {
				String regex = column.getRegex();
				if (regex != null) {
					try {
						Pattern compiledRegex = Pattern.compile(regex);
						regexCache.put(regex, compiledRegex);
					} catch (PatternSyntaxException e) {
						LOGGER.error("Regex invalid for file {} column {}", file.getName(), column.getName(), e);
					}
				}
			}
		}
		return regexCache;
	}

}
