package org.ihtsdo.rvf.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts entities into their json equivalents to present to ember
 */
public class JsonEntityGenerator {

	@Autowired
	private ObjectMapper objectMapper;

	public List<Map<String, Object>> getEntityCollection(
			List<? extends Object> entities, HttpServletRequest request) {
		List<Map<String, Object>> jsonEntities = new ArrayList<>();

		for (Object entity : entities) {
			jsonEntities.add(getEntity(entity));
		}

		return jsonEntities;
	}

	public Map<String, Object> getEntity(Object entity) {
		return objectMapper.convertValue(entity, Map.class);
	}

}
