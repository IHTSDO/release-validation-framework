package org.ihtsdo.snomed.rvf.importer.helper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * A simple REST client that communicates with the RVF API. This class is used to populate the RVF database.
 */
@Service
public class RvfRestClient {

	private static final Logger logger = LoggerFactory.getLogger(RvfRestClient.class);
	//private final static String defaultServerUrl = "https://dev-rvf.ihtsdotools.org/api/v1/";
	private final static String defaultServerUrl = "http://localhost:8080/api/v1/";
	private String serverUrl;
	private final RestTemplate restTemplate;
	private final HttpHeaders headers;

	public RvfRestClient() {
		this.restTemplate = new RestTemplate();
		/*this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
			protected boolean hasError(HttpStatus statusCode) {
				return false; }
			public void handleError(ClientHttpResponse response) {
				return;
			}
		});*/
		this.headers = new HttpHeaders();
		headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
		// verify server url
		setServerUrl(defaultServerUrl);
	}

	public ResponseEntity get(final String uri) {
		final HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
		return restTemplate.exchange(serverUrl + uri, HttpMethod.GET, requestEntity, String.class);
	}

	public ResponseEntity post(final String uri, final String json) {
		final HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);
		return restTemplate.exchange(serverUrl + uri, HttpMethod.POST, requestEntity, String.class);
	}

	public ResponseEntity put(final String uri, final String json, final Class clazz) {
		final HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);
		return restTemplate.exchange(serverUrl + uri, HttpMethod.PUT, requestEntity, clazz);
	}

	public ResponseEntity delete(final String uri, final Class clazz) {
		final HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
		return restTemplate.exchange(serverUrl + uri, HttpMethod.DELETE, requestEntity, clazz);
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(final String serverUrl) {
		this.serverUrl = serverUrl;
		try {
			final HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
			connection.setRequestMethod("HEAD");
			final int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				logger.error("Unsuccessful connection to {}, Response code: {} ", serverUrl, responseCode);
			}
		} catch (final IOException e) {
			logger.warn("Exception while testing connection to {}", serverUrl, e);
		}
	}
}
