package org.ihtsdo.rvf.executionservice.traceability;

import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URISyntaxException;

public class TraceabilityServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(TraceabilityServiceClient.class);

    private String traceabilityServiceUrl;
    private RestTemplate restTemplate;
    private HttpHeaders headers;

    private static final ParameterizedTypeReference<ChangeSummaryReport> CHANGE_SUMMARY_REPORT_TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

    public TraceabilityServiceClient(String traceabilityServiceUrl, String authToken) {
        this.traceabilityServiceUrl = traceabilityServiceUrl;
        headers = new HttpHeaders();
        headers.add("Cookie", authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate = new RestTemplateBuilder()
                .rootUri(this.traceabilityServiceUrl)
                .additionalMessageConverters(new GsonHttpMessageConverter())
                .errorHandler(new ExpressiveErrorHandler())
                .build();

        restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor(){
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                request.getHeaders().addAll(headers);
                return execution.execute(request, body);
            }
        });
    }

    public static TraceabilityServiceClient createClient(String traceabilityServiceUrl, String imsUrl, String username, String password) {
        IMSRestClient imsClient = new IMSRestClient(imsUrl);
        String token = null;
        try {
            token = imsClient.login(username, password);
            PreAuthenticatedAuthenticationToken decoratedAuthentication = new PreAuthenticatedAuthenticationToken(username, token);
            SecurityContextHolder.getContext().setAuthentication(decoratedAuthentication);
            return new TraceabilityServiceClient(traceabilityServiceUrl, token);
        } catch (IOException | URISyntaxException e) {
            logger.error("Error while trying to login. Message: {}", e.getMessage());
        }
        return null;
    }

    public ChangeSummaryReport getTraceabilityChangeSummaryReport(String branchPath, Long contentBaseTimeStamp, Long contentHeadTimestamp) {
        logger.info("Fetching diff from traceability service..");
        final String uri = UriComponentsBuilder.fromPath("/change-summary")
                .queryParam("branch", branchPath)
                .queryParam("contentBaseTimestamp", contentBaseTimeStamp)
                .queryParam("contentHeadTimestamp", contentHeadTimestamp).toUriString();
        final ResponseEntity<ChangeSummaryReport> response = restTemplate.exchange(uri, HttpMethod.GET, null, CHANGE_SUMMARY_REPORT_TYPE_REFERENCE);
        return response.getBody();
    }
}
