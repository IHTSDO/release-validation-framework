package org.ihtsdo.rvf.helper;

import java.io.IOException;

import org.ihtsdo.rvf.entity.AssertionGroup;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A custom {@link org.springframework.core.convert.converter.Converter} for
 * handling {@link org.ihtsdo.rvf.entity.AssertionGroup}s.
 */
@Component
public class AssertionGroupConverter implements
		Converter<String, AssertionGroup> {

	ObjectMapper mapper = new ObjectMapper();

	@Override
	public AssertionGroup convert(String s) {
		try {
			return mapper.readValue(s, AssertionGroup.class);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(
					"String version passed can not be null.");
		}
	}
}
