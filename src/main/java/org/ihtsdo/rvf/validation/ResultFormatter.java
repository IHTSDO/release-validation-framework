package org.ihtsdo.rvf.validation;

import java.util.List;

public interface ResultFormatter {

	String formatResults(List<StructuralTestRunItem> results);

	String formatRow(StructuralTestRunItem testRunItem, Integer itemErrorCount);

	String getHeaders();

}
