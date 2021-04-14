package org.ihtsdo.rvf.execution.service.whitelist;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    public AcceptanceGatewayClient getClient(){
        return AcceptanceGatewayClient.createClient(acceptanceGatewayServiceUrl, imsUrl, username, password);
    }

    public boolean isWhitelistingDisabled() {
        return StringUtils.isEmpty(acceptanceGatewayServiceUrl);
    }
}

