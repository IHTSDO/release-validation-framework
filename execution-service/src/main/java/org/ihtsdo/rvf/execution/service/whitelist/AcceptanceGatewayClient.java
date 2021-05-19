package org.ihtsdo.rvf.execution.service.whitelist;

import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.otf.rest.client.ims.IMSRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class AcceptanceGatewayClient {
    private static final Logger logger = LoggerFactory.getLogger(AcceptanceGatewayClient.class);

    private String acceptanceGatewayServiceUrl;
    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private static final ParameterizedTypeReference<List<WhitelistItem>> WHITELIST_ITEM_LIST_TYPE_REFERENCE = new ParameterizedTypeReference<List<WhitelistItem>>() {};

    public AcceptanceGatewayClient(String acceptanceGatewayServiceUrl, String authToken) throws URISyntaxException, IOException {
        this.acceptanceGatewayServiceUrl = acceptanceGatewayServiceUrl;
        headers = new HttpHeaders();
        headers.add("Cookie", authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate = new RestTemplateBuilder()
                .rootUri(this.acceptanceGatewayServiceUrl)
                .additionalMessageConverters(new GsonHttpMessageConverter())
                .errorHandler(new ExpressiveErrorHandler())
                .build();

        //Add a ClientHttpRequestInterceptor to the RestTemplate to add cookies as required
        restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor(){
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                request.getHeaders().addAll(headers);
                return execution.execute(request, body);
            }
        });
    }

    public static AcceptanceGatewayClient createClient(String acceptanceGatewayServiceUrl, String imsUrl, String username, String password) {
        IMSRestClient imsClient = new IMSRestClient(imsUrl);
        String token = null;
        try {
            token = imsClient.login(username, password);
            PreAuthenticatedAuthenticationToken decoratedAuthentication = new PreAuthenticatedAuthenticationToken(username, token);
            SecurityContextHolder.getContext().setAuthentication(decoratedAuthentication);
            return new AcceptanceGatewayClient(acceptanceGatewayServiceUrl, token);
        } catch (IOException | URISyntaxException e) {
            logger.error("Error while trying to login. Message: {}", e.getMessage());
        }
        return null;
    }

    public List<WhitelistItem> checkComponentFailureAgainstWhitelist(List<WhitelistItem> items) throws RestClientException {
        ResponseEntity<List<WhitelistItem>> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(this.acceptanceGatewayServiceUrl + "/whitelist-items/bulk-validate", HttpMethod.POST, new org.springframework.http.HttpEntity<>(items), WHITELIST_ITEM_LIST_TYPE_REFERENCE);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorMessage = "Failed to validate the component failures against AAG. Error message: " + e.getMessage();
            logger.error(errorMessage);
            throw new RestClientException(errorMessage);
        }
        return responseEntity.getBody();
    }
}

