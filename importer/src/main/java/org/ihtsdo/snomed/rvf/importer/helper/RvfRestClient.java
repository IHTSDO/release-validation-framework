package org.ihtsdo.snomed.rvf.importer.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A simple REST client that communicates with the RVF API. This class is used to populate the RVF database.
 */
@Service
public class RvfRestClient {

    private static final Logger logger = LoggerFactory.getLogger(RvfRestClient.class);
    private String serverUrl = "http://localhost:8080/api/v1/";
    @Autowired
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    public RvfRestClient() {
        this.restTemplate = new RestTemplate();
        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "*/*");
        // verify server url
        setServerUrl(serverUrl);
    }

    public ResponseEntity get(String uri) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
        return restTemplate.exchange(serverUrl + uri, HttpMethod.GET, requestEntity, String.class);
    }

    public ResponseEntity post(String uri, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);
        return restTemplate.exchange(serverUrl + uri, HttpMethod.POST, requestEntity, String.class);
    }

    public ResponseEntity put(String uri, String json, Class clazz) {
        HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);
        return restTemplate.exchange(serverUrl + uri, HttpMethod.PUT, requestEntity, clazz);
    }

    public ResponseEntity delete(String uri, Class clazz) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
        return restTemplate.exchange(serverUrl + uri, HttpMethod.DELETE, requestEntity, clazz);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.error("Error connecting to serverUrl specified :" + serverUrl);
            }
        }
        catch (IOException e) {
            logger.warn("Nested exception is : " + e.fillInStackTrace());
        }
    }
}
