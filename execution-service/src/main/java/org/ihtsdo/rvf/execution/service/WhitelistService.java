package org.ihtsdo.rvf.execution.service;

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

    public  List<WhitelistItem> validateAssertions(List<WhitelistItem> assertions) {
        if (isWhitelistDisabled()) {
            return Collections.emptyList();
        }

        AcceptanceGatewayClient client = factory.getClient();
        return client.validateAssertions(assertions);
    }
}
