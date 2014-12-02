package org.ihtsdo.rvf.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/version")
public class VersionController {

	public static final String VERSION_FILE_PATH = "/var/opt/snomed-release-service-api/version.txt";

	private String versionString;

	@RequestMapping
	@ResponseBody
	public Map<String, String> getVersion(HttpServletRequest request) throws IOException {
		Map<String, String> entity = new HashMap<>();
		entity.put("package_version", getVersionString());
		return entity;//hypermediaGenerator.getEntityHypermedia(entity, true, request, new String[]{});
	}

	private String getVersionString() throws IOException {
		if (this.versionString == null) {
			String versionString = "";
			File file = new File(VERSION_FILE_PATH);
			if (file.isFile()) {
				try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
					versionString = bufferedReader.readLine();
				}
			} else {
				versionString = "Version information not found.";
			}
			this.versionString = versionString;
		}
		return versionString;
	}

}
