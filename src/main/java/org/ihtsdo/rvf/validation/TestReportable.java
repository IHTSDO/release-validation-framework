package org.ihtsdo.rvf.validation;

import java.util.Date;
import java.util.List;

public interface TestReportable {

	void addError(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern, String actualValue, String expectedValue, Long lineNr);
	
	void addSuccess(String executionId, Date testTime, String fileName, String filePath, String columnName, String testType, String testPattern);

	String getResult();

	int getNumErrors();

	int getNumSuccesses();

	int getNumTestRuns();

	int getNumberRecordedErrors();

	String writeSummary();

	void addNewLine();
	
	List<StructuralTestRunItem> getFailedItems();

	class TestRunItemCount {
		private StructuralTestRunItem item;
		private Integer count = 0;

		public TestRunItemCount(StructuralTestRunItem item) {
			this.item = item;
			count = 1;
		}

		public void addError() {
			count++;
		}

		public Integer getErrorCount() {
			return count;
		}

		public StructuralTestRunItem getItem() {
			return item;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TestRunItemCount that = (TestRunItemCount) o;
			return count.equals(that.count) && item.equals(that.item);
		}

		@Override
		public int hashCode() {
			int result = item.hashCode();
			result = 31 * result + count.hashCode();
			return result;
		}
	}

}
