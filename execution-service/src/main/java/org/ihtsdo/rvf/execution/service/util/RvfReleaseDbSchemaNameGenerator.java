package org.ihtsdo.rvf.execution.service.util;

import java.util.HashMap;
import java.util.Map;

public class RvfReleaseDbSchemaNameGenerator {
	
	private static final Map<String,String> REPLACEMENTS = new HashMap<>();
	static {
		REPLACEMENTS.put("snomedCT", "");
		REPLACEMENTS.put("xsnomedct", "");
		REPLACEMENTS.put("release", "");
		REPLACEMENTS.put("production", "prod");
		REPLACEMENTS.put("international", "int");
		REPLACEMENTS.put("-", "");
		
	}
	public static String generate(String releaseFilename) {
		if (releaseFilename != null && !releaseFilename.endsWith(".zip")) {
			return releaseFilename.replaceAll("-", "_");
		}
		releaseFilename = releaseFilename.replace(".zip", "");
		String[] splits = releaseFilename.split("_", -1);
		for (int i = 0; i < splits.length; i++) {
			splits[i] = splits[i].toLowerCase();
			for (String word : REPLACEMENTS.keySet()) {
				if (splits[i].contains(word.toLowerCase())) {
					splits[i] = splits[i].replaceAll(word.toLowerCase(), REPLACEMENTS.get(word));
				}
			}
		}
		StringBuilder builder = new StringBuilder();
		for (String part : splits) {
			if (!part.trim().isEmpty()) {
				if (builder.length() > 0) {
					builder.append("_");
				}
				builder.append(part);
			}
		}
		return "rvf_" + builder.toString();
	}
}
