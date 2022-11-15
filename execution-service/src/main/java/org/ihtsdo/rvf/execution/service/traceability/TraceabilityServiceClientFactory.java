package org.ihtsdo.rvf.execution.service.traceability;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
@Service
public class TraceabilityServiceClientFactory {

    @Value("${ims.url}")
    private String imsUrl;

    @Value("${rvf.ims.username}")
    private String username;

    @Value("${rvf.ims.password}")
    private String password;
    private final String traceabilityServiceUrl;
    private final Cache<String, TraceabilityServiceClient> clientCache;

    public TraceabilityServiceClientFactory(@Value("${traceability-service.url}") String traceabilityServiceUrl) {
        this.traceabilityServiceUrl = traceabilityServiceUrl;
        clientCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
    }

    public TraceabilityServiceClient getClient() {
        TraceabilityServiceClient client = null;
        String authenticationToken = SecurityUtil.getAuthenticationToken();
        if (!StringUtils.isEmpty(authenticationToken)) {
            client = this.clientCache.getIfPresent(authenticationToken);
        }
        if (client == null) {
            synchronized (clientCache) {
                client = TraceabilityServiceClient.createClient(traceabilityServiceUrl, imsUrl, username, password);
                authenticationToken = SecurityUtil.getAuthenticationToken();
                clientCache.put(authenticationToken, client);
            }
        }
        return client;
    }
}
