package org.ihtsdo.rvf.core.service.pojo;

import org.ihtsdo.rvf.core.service.ValidationReportService;
import org.ihtsdo.rvf.core.service.config.ValidationRunConfig;
import org.springframework.util.StringUtils;

import java.util.StringJoiner;

public class ValidationStatusResponse {
	private final Long runId;
	private final String state;
	private final String username;
	private final String authenticationToken;

	public ValidationStatusResponse(ValidationRunConfig config, ValidationReportService.State state) {
		this.runId = config.getRunId();
		this.state = state.name();
		this.username = config.getUsername() != null ? config.getUsername() : "";
		this.authenticationToken = config.getAuthenticationToken() != null ? config.getAuthenticationToken() : "";
	}

	public Long getRunId() {
		return runId;
	}

	public String getState() {
		return state;
	}

	public String getUsername() {
		return username;
	}

	public String getAuthenticationToken() {
		return authenticationToken;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ValidationStatusResponse.class.getSimpleName() + "[", "]")
				.add("runId=" + runId)
				.add("state='" + state + "'")
				.add("username='" + username + "'")
				.add("authenticationToken='" + mask(authenticationToken) + "'").toString();
	}

	private String mask(String token) {
		if (StringUtils.isEmpty(token)) {
			return null;
		}
		int start = 1;
		if (token.contains("=")) {
			start = token.indexOf("=");
		}
		char[] maskedToken = new char[token.length()];
		for (int i = 0; i < maskedToken.length; i++) {
			maskedToken[i] = '*';
		}
		for (int j = 0; j < start; j++) {
			maskedToken[j] = token.charAt(j);
		}
		maskedToken[maskedToken.length - 1] = token.charAt(token.length() - 1);
		return new String(maskedToken);
	}
}
