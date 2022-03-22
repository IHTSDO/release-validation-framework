package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.rvf.execution.service.config.ValidationRunConfig;

import java.util.StringJoiner;

public class ValidationStatusResponse {
	private Long runId;
	private String state;
	private String username;
	private String authenticationToken;

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
		if (token == null) {
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
