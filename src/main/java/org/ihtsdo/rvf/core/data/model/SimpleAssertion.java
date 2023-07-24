package org.ihtsdo.rvf.core.data.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * Simplified version of assertion assuming one test for easier file / user interface access.
 */
public class SimpleAssertion {
	
	String shortName;
	String assertionText;
	UUID id;
	ArrayList<String[]> tests;
	String docRef;
	String keywords;
	
	public String getShortName() {
		return shortName;
	}
	
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
	
	public String getAssertionText() {
		return assertionText;
	}
	
	public void setAssertionText(String assertionText) {
		this.assertionText = assertionText;
	}
	
	public UUID getId() {
		return id;
	}
	
	public void setId(UUID id) {
		this.id = id;
	}
	
	public ArrayList<String[]> getTests() {
		return tests;
	}
	
	public void setTests(ArrayList<String[]> test) {
		this.tests = test;
	}
	
	public String getDocRef() {
		return docRef;
	}
	
	public void setDocRef(String docRef) {
		this.docRef = docRef;
	}
	
	public Assertion toAssertion() {
		Assertion assertion = new Assertion();
		assertion.setAssertionText(assertionText);
		assertion.setUuid(id);
		assertion.setShortName(shortName);
		assertion.setKeywords(keywords);
		assertion.setDocRef(docRef);
		return assertion;
	}
	
	public String getKeywords() {
		return keywords;
	}
	
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	public List<String> getTestsAsList() {
		ArrayList<String> sqls = new ArrayList<String>();
		for (String[] sqlLines : tests) {
			sqls.add(StringUtils.join(sqlLines, "\n"));
		}
		return sqls;
	}

}
