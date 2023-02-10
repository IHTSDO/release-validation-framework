package org.ihtsdo.rvf.core.validation;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestWriterDelegate extends PrintWriter {
	private StringBuffer buffer = new StringBuffer();

	public TestWriterDelegate(StringWriter out) {
		super(out);
	}

	@Override
	public void write(String s) {
		super.write(s);
		buffer.append(s);
	}

	@Override
	public String toString() {
		return buffer.toString();
	}

}
