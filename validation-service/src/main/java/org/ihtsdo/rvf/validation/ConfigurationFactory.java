package org.ihtsdo.rvf.validation;

import org.ihtsdo.release.assertion.ResourceProviderFactory;
import org.ihtsdo.release.assertion.log.ValidationLog;
import org.ihtsdo.rvf.assertion._1_0.Column;
import org.ihtsdo.rvf.assertion._1_0.ColumnPatternConfiguration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 */
public class ConfigurationFactory {

    private Map<Pattern, ColumnPatternConfiguration> configurations;
    private Map<String, Pattern> regexCache;
    private ValidationLog validationLog;

    public ConfigurationFactory(Map<String, String> filenamePatternToConfigMap, ResourceProviderFactory resourceProviderFactory) {
        this.configurations = new HashMap<>();
        this.validationLog = resourceProviderFactory.getValidationLog(this.getClass());
        for (Map.Entry<String, String> entry : filenamePatternToConfigMap.entrySet()) {
            ColumnPatternConfiguration configuration = loadConfiguration(entry.getValue());
            configurations.put(Pattern.compile(entry.getKey()), configuration);
            regexCache = testConfigurationByPrecompilingRegexPatterns(configuration);
        }
    }

    public ColumnPatternConfiguration getConfiguration(String fileName) {
        for (Map.Entry<Pattern, ColumnPatternConfiguration> entry : configurations.entrySet()) {
            if(entry.getKey().matcher(fileName).matches()) return entry.getValue();
        }
        return null;
    }

    public Map<String, Pattern> getRegexCache() {
        return regexCache;
    }

    public ColumnPatternConfiguration getConfiguration(Pattern pattern) {
        for (Map.Entry<Pattern, ColumnPatternConfiguration> entry : configurations.entrySet()) {
            if(entry.getKey().pattern().equals(pattern.pattern())) return entry.getValue();
        }
        return null;
    }

    private ColumnPatternConfiguration loadConfiguration(String filename) {

        try {
            InputStream configurationStream = getClass().getResourceAsStream(filename);

            JAXBContext jaxbContext = JAXBContext.newInstance(ColumnPatternConfiguration.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<ColumnPatternConfiguration> configurationJAXBElement = unmarshaller.unmarshal(new StreamSource(configurationStream), ColumnPatternConfiguration.class);
            return configurationJAXBElement.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Pattern> testConfigurationByPrecompilingRegexPatterns(ColumnPatternConfiguration configuration) {
        Map<String, Pattern> regexCache = new HashMap<>();
        for (ColumnPatternConfiguration.File file : configuration.getFile()) {
            for (Column column : file.getColumn()) {
                String regex = column.getRegex();
                if (regex != null) {
                    try {
                        Pattern compiledRegex = Pattern.compile(regex);
                        regexCache.put(regex, compiledRegex);
                    } catch (PatternSyntaxException e) {
                        validationLog.configurationError("Regex invalid for file {} column {}", file.getName(), column.getName(), e);
                    }
                }
            }
        }
        return regexCache;
    }
}
