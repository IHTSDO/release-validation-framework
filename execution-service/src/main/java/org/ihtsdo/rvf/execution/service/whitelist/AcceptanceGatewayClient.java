package org.ihtsdo.rvf.execution.service.whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.rest.client.authoringservices.RestyOverrideAccept;
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
import us.monoid.web.Resty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class AcceptanceGatewayClient {
    private static final Logger logger = LoggerFactory.getLogger(AcceptanceGatewayClient.class);

    private String acceptanceGatewayServiceUrl;
    private RestTemplate restTemplate;
    private HttpHeaders headers;
    private Resty resty;
    private static final String ALL_CONTENT_TYPE = "*/*";
    private static final ParameterizedTypeReference<List<WhitelistItem>> WHITELIST_ITEM_LIST_TYPE_REFERENCE = new ParameterizedTypeReference<List<WhitelistItem>>() {};

    protected static Gson gson;
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.disableHtmlEscaping();
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        gson = gsonBuilder.create();
    }

    public AcceptanceGatewayClient(String acceptanceGatewayServiceUrl, String authToken) throws URISyntaxException, IOException {
        this.acceptanceGatewayServiceUrl = acceptanceGatewayServiceUrl;

        resty = new Resty(new RestyOverrideAccept(ALL_CONTENT_TYPE));
        resty.withHeader("Cookie", authToken);
        resty.withHeader("Connection", "close");
        resty.authenticate(this.acceptanceGatewayServiceUrl, null,null);

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

    public List<WhitelistItem> validateAssertions(List<WhitelistItem> assertions) {
        ResponseEntity<List<WhitelistItem>> responseEntity = restTemplate.exchange(this.acceptanceGatewayServiceUrl + "/whitelist-items/bulk-validate", HttpMethod.POST, new org.springframework.http.HttpEntity<>(assertions), WHITELIST_ITEM_LIST_TYPE_REFERENCE);
        return responseEntity.getBody();
    }
}

