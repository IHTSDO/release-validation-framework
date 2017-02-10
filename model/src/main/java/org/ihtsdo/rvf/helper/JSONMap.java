package org.ihtsdo.rvf.helper;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Wrapper around a JSONObject to allow it to be returned in a response and serialized as expected
 * @author Peter
 *
 */
public class JSONMap implements Map<String, Object> {
	
	private JSONObject jsonObject;
	
	public JSONMap (JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public int size() {
		return jsonObject.length();
	}

	@Override
	public boolean isEmpty() {
		return (size()==0);
	}

	@Override
	public boolean containsKey(Object key) {
		return jsonObject.has(key.toString());
	}

	@Override
	public boolean containsValue(Object value) {
		//I think we'd have to iterate through all the keys and check the objects recovered 
		//to implement this function.
		return false;
	}

	@Override
	public Object get(Object key) {
		Object obj = jsonObject.get(key.toString());
		if (obj instanceof JSONObject) {
			return new JSONMap((JSONObject)obj);
		} else if (obj instanceof JSONArray) {
			return new JSONMapArray((JSONArray)obj);
		}	else {
			return obj;
		}
	}

	@Override
	public Object put(String key, Object value) {
		return jsonObject.put (key, value);
	}

	@Override
	public Object remove(Object key) {
		Object recoveredObj = null;
		try {
			recoveredObj = jsonObject.get (key.toString());
			jsonObject.remove (key.toString());
		} catch (JSONException e) {
			return null;
		}
		return recoveredObj;
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<String> keySet() {
		return jsonObject.keySet();
	}

	@Override
	public Collection<Object> values() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		Set<java.util.Map.Entry<String, Object>> entrySet = new HashSet<Entry<String, Object>>();
		for (String key : keySet()) {
			java.util.Map.Entry<String, Object> thisEntry =
			new AbstractMap.SimpleEntry<String, Object>(key, get(key));
			entrySet.add(thisEntry);
		}
		return entrySet;
	}

}
