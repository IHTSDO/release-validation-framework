package org.ihtsdo.rvf.service.mapping;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonObjectMapperFactory {

	/**
	 * Creates the jackson ObjectMapper
	 *
	 * @return ObjectMapper with required configuration enabled
	 */
	public ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		return objectMapper;
	}

}
