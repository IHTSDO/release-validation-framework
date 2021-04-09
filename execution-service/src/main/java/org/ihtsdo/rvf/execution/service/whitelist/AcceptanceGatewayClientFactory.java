package org.ihtsdo.rvf.execution.service.whitelist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;

@Service
public class AcceptanceGatewayClientFactory {

    @Value("${acceptance.gateway.url}")
    private String acceptanceGatewayServiceUrl;

    @Value("${ims.url}")
    private String imsUrl;

    @Value("${ims.username}")
    private String username;

    @Value("${ims.password}")
    private String password;

    public AcceptanceGatewayClient getClient() throws IOException, URISyntaxException {
        return AcceptanceGatewayClient.createClient(acceptanceGatewayServiceUrl, imsUrl, username, password);
    }
}

