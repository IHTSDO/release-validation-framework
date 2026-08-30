package org.ihtsdo.rvf.core.service.structure.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ResourceProvider {

	BufferedReader getReader(String name, Charset charset) throws IOException;

	String getFilePath();

	List<String> getFileNames();

	/**
	 * The uncompressed size of an entry in bytes, or -1 when it is not known.
	 *
	 * <p>Used only to order work largest-first. Default -1 so an implementation
	 * that cannot cheaply answer keeps the previous, unordered behaviour.
	 */
	default long getFileSize(String name) {
		return -1L;
	}

	/**
	 * The same files, biggest first.
	 *
	 * <p>RF2 file sizes are extremely skewed: on a real edition of 76 files and
	 * 5.3GB, the largest single file is 20% of all bytes and the top eight are
	 * 82%, leaving 68 files to share the remaining 18%. Both structural testers
	 * run one task per file, so submitted in directory order the small files
	 * finish early while the giants are still starting, and the pool drains to
	 * two or three busy threads for the rest of the phase - measured at 2.6 of 8
	 * cores.
	 *
	 * <p>Longest-processing-time-first is the standard remedy for that, and here
	 * it is just a comparator: start the giants immediately and let the small
	 * files fill in behind them. Ordering cannot change any result, only the order
	 * in which files are validated.
	 *
	 * <p>Implementations that cannot report sizes return -1 from
	 * {@link #getFileSize} and keep their original order.
	 */
	default List<String> getFileNamesLargestFirst() {
		List<String> fileNames = getFileNames();
		Map<String, Long> sizes = new HashMap<>();
		boolean anyKnown = false;
		for (String name : fileNames) {
			long size = getFileSize(name);
			sizes.put(name, size);
			anyKnown |= size >= 0;
		}
		if (!anyKnown) {
			return fileNames;
		}
		List<String> ordered = new ArrayList<>(fileNames);
		ordered.sort(Comparator.comparingLong((String n) -> sizes.getOrDefault(n, -1L)).reversed());
		return ordered;
	}

	boolean match(String name);

}
