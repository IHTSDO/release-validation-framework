package org.ihtsdo.rvf.helper;

import org.ihtsdo.rvf.helper.JSONMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONMapTest {
	
	JSONObject testJSONObject;
	@Before	
	public void setup() {
		testJSONObject = new JSONObject();
		testJSONObject.put("hello", "world");
		
		JSONArray testArray = new JSONArray();
		testArray.put ("first");
		testArray.put("second");
		testArray.put("third");
		testJSONObject.put("myArray", testArray);
	}

	@Test
	public void test() throws JsonProcessingException {
		//try serializing our JSONObject by wrapping it in a JSONMap
		JSONMap map = new JSONMap(testJSONObject);
		ObjectMapper mapper = new ObjectMapper();
		//If this call completes without throwing an exception, then our code
		//has performed as expected.
		mapper.writeValueAsString(map);
	}

}
