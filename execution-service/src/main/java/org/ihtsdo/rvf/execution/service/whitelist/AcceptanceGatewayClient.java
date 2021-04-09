package org.ihtsdo.rvf.execution.service.whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ihtsdo.otf.rest.client.ExpressiveErrorHandler;
import org.ihtsdo.otf.rest.client.authoringservices.RestyOverrideAccept;
import org.ihtsdo.otf.rest.client.ims.IMSRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import us.monoid.web.Resty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

public class AcceptanceGatewayClient {
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

    public static AcceptanceGatewayClient createClient(String acceptanceGatewayServiceUrl, String imsUrl, String username, String password) throws URISyntaxException, IOException  {
        IMSRestClient imsClient = new IMSRestClient(imsUrl);
        String token = imsClient.loginForceNewSession(username, password);
        return new AcceptanceGatewayClient(acceptanceGatewayServiceUrl, token);
    }

    public List<WhitelistItem> getWhitelistedItems(Set<WhitelistItem> items) {
        ResponseEntity<List<WhitelistItem>> responseEntity = restTemplate.exchange(acceptanceGatewayServiceUrl + "/bulk-validate", HttpMethod.POST, new org.springframework.http.HttpEntity<>(items), WHITELIST_ITEM_LIST_TYPE_REFERENCE);
        return responseEntity.getBody();
    }
}

