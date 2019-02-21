package org.ihtsdo.rvf.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@RestController
@RequestMapping("/version")
@Api(position = 6, value = "RVF api version")
public class VersionController {

	public static final String VERSION_FILE_PATH = "/opt/rvf-api/data/version.txt";

	private String versionString;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@ApiOperation(value = "Get the RVF api version", notes = "This api is used to get the deployed RVF version. "
			+ "It looks for the version number stored in /opt/rvf-api/data/version.txt.")
	public Map<String, String> getVersion(HttpServletRequest request, UriComponentsBuilder uriComponentsBuilder) throws IOException {
		Map<String, String> entity = new HashMap<>();
		entity.put("package_version", getVersionString());
		entity.put("request_url", URI.create(uriComponentsBuilder.toUriString()).toURL().toString());
		return entity;
	}

	private String getVersionString() throws IOException {
		if (this.versionString == null) {
			String versionString = "";
			File file = new File(VERSION_FILE_PATH);
			if (file.isFile()) {
				try (BufferedReader bufferedReader = new BufferedReader(
						new FileReader(file))) {
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
