package org.ihtsdo.rvf.service.mapping;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class JacksonObjectMapperFactory {

    /**
     * Creates the jackson ObjectMapper
     * @return ObjectMapper with required configuration enabled
     */
    public ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        return objectMapper;
    }
}
