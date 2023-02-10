package org.ihtsdo.rvf.core.service.structure.validation;

import java.util.List;

public interface ResultFormatter {

	String formatResults(List<StructuralTestRunItem> results);

	String formatRow(StructuralTestRunItem testRunItem, Integer itemErrorCount);

	String getHeaders();

}
