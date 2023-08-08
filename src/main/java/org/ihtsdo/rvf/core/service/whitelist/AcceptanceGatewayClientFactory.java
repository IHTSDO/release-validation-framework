package org.ihtsdo.rvf.core.service.whitelist;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class AcceptanceGatewayClientFactory {

    @Value("${aag.url}")
    private String acceptanceGatewayServiceUrl;

    @Value("${ims.url}")
    private String imsUrl;

    @Value("${rvf.ims.username}")
    private String username;

    @Value("${rvf.ims.password}")
    private String password;

    private final Cache<String, AcceptanceGatewayClient> clientCache;

    public AcceptanceGatewayClientFactory() {
       this.clientCache = CacheBuilder.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).build();
    }

    public AcceptanceGatewayClient getClient(){
        AcceptanceGatewayClient client = null;
        String authenticationToken = SecurityUtil.getAuthenticationToken();
        if (!StringUtils.isEmpty(authenticationToken)) {
            client = this.clientCache.getIfPresent(authenticationToken);
        }
        if (client == null) {
            synchronized(this.clientCache) {
                authenticationToken = SecurityUtil.getAuthenticationToken();
                if (!StringUtils.isEmpty(authenticationToken)) {
                    client = this.clientCache.getIfPresent(authenticationToken);
                }
                if (client == null) {
                    client = AcceptanceGatewayClient.createClient(acceptanceGatewayServiceUrl, imsUrl, username, password);
                    authenticationToken = SecurityUtil.getAuthenticationToken();
                    this.clientCache.put(authenticationToken, client);
                }
            }
        }

        return client;
    }

    public boolean isWhitelistDisabled() {
        return StringUtils.isEmpty(acceptanceGatewayServiceUrl);
    }
}

