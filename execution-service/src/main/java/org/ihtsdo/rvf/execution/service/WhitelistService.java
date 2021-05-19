package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.ihtsdo.rvf.execution.service.whitelist.AcceptanceGatewayClient;
import org.ihtsdo.rvf.execution.service.whitelist.AcceptanceGatewayClientFactory;
import org.ihtsdo.rvf.execution.service.whitelist.WhitelistItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class WhitelistService {

    @Autowired
    private AcceptanceGatewayClientFactory factory;

    public boolean isWhitelistDisabled() {
        return factory.isWhitelistDisabled();
    }

    /**
     * Check which component validation failures are on the AAG whitelist.
     * @param whitelistItems The list of whitelist item to be checked
     * @return The list of whitelisted items
     */
    public  List<WhitelistItem> checkComponentFailuresAgainstWhitelist(List<WhitelistItem> whitelistItems) throws RestClientException {
        if (isWhitelistDisabled()) {
            return Collections.emptyList();
        }

        AcceptanceGatewayClient client = factory.getClient();
        return client.checkComponentFailureAgainstWhitelist(whitelistItems);
    }
}
