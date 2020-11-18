package org.ihtsdo.rvf.validation.model.manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;

public class ManifestValidator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ManifestValidator.class);
	public static String validate(InputStream manifestInputStream) {
		String failureMessage = null;
		if (manifestInputStream == null) {
			failureMessage = "manifest inputstream can't be null";
			return failureMessage;
		}
		try
	    {
	        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	        Schema schema = factory.newSchema(ManifestValidator.class.getClassLoader().getResource("manifest.xsd"));
	        Validator validator = schema.newValidator();
	        validator.validate(new StreamSource(manifestInputStream));
	    }
	    catch (Exception e) {
	    	String msg = "Errors found when validating manifest.xml";
	    	LOGGER.error(msg, e);
	    	failureMessage = e.getMessage();
	    	if (failureMessage == null && e.getCause() != null) {
	    		failureMessage = e.getCause().getMessage() != null ? e.getCause().getMessage() : msg;
	    	}
	    	if (e instanceof SAXParseException) {
	    		StringBuilder msgBuilder = new StringBuilder();
	    		msgBuilder.append(failureMessage);
	    		msgBuilder.append(" The issue lies in the manifest.xml at line " + ((SAXParseException) e).getLineNumber());
	    		msgBuilder.append(" and column " + ((SAXParseException) e).getColumnNumber());
	    		failureMessage = msgBuilder.toString();
	    	}
	    }
		return failureMessage;
	}
}
