//package org.ihtsdo.rvf.helper;
//
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.Set;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//public class JSONMapArray implements Set<Object> {
//	
//	JSONArray jsonArray;
//	
//	public JSONMapArray (JSONArray jsonArray) {
//		this.jsonArray = jsonArray;
//	}
//
//	@Override
//	public int size() {
//		return jsonArray.length();
//	}
//
//	@Override
//	public boolean isEmpty() {
//		return (size() == 0);
//	}
//
//	@Override
//	public boolean contains(Object o) {
//		throw new RuntimeException ("Method not yet implemented");
//		// return false;
//	}
//
//	@Override
//	public Iterator<Object> iterator() {
//		return new JSONMapArrayIterator(jsonArray);
//	}
//
//	@Override
//	public Object[] toArray() {
//		throw new RuntimeException ("Method not yet implemented");
//		// return null;
//	}
//
//	@Override
//	public <T> T[] toArray(T[] a) {
//		throw new RuntimeException ("Method not yet implemented");
//		// return null;
//	}
//
//	@Override
//	public boolean add(Object e) {
//		throw new RuntimeException ("Method not yet implemented");
//		// return false;
//	}
//
//	@Override
//	public boolean remove(Object o) {
//		throw new RuntimeException ("Method not yet implemented");
//		// return false;
//	}
//
//	@Override
//	public boolean containsAll(Collection<?> c) {
//		throw new RuntimeException ("Method not yet implemented");
//		// return false;
//	}
//
//	@Override
//	public boolean addAll(Collection<? extends Object> c) {
//		throw new RuntimeException ("Method not yet implemented");
//		//// return false;
//	}
//
//	@Override
//	public boolean retainAll(Collection<?> c) {
//		throw new RuntimeException ("Method not yet implemented");
//		// return false;
//	}
//
//	@Override
//	public boolean removeAll(Collection<?> c) {
//		throw new RuntimeException ("Method not yet implemented");
//		// return false;
//	}
//
//	@Override
//	public void clear() {
//		jsonArray = new JSONArray();
//
//	}
//	
//	public class JSONMapArrayIterator implements Iterator<Object> {
//		
//		public JSONArray jsonArray;
//		int currentPointer = 0;
//		
//		public JSONMapArrayIterator (JSONArray jsonArray) {
//			this.jsonArray = jsonArray ;
//		}
//
//		@Override
//		public boolean hasNext() {
//			return currentPointer < jsonArray.length();
//		}
//
//		@Override
//		public Object next() {
//			Object retVal = jsonArray.get(currentPointer);
//			currentPointer++;
//			
//			if (retVal instanceof JSONObject) {
//				retVal = new JSONMap((JSONObject)retVal);
//			} else if (retVal instanceof JSONArray) {
//				retVal = new JSONMapArray((JSONArray)retVal);
//			}	else {
//				// return object as is.
//			}
//			
//			return retVal;
//		}
//
//		@Override
//		public void remove() {
//			throw new RuntimeException ("Method not yet implemented");
//		}
//		
//	}
//
//}
