package org.ihtsdo.rvf.execution.service.whitelist;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.List;

public class WhitelistAdapter {
    WhitelistReport whitelist;

    @Autowired
    private AcceptanceGatewayClientFactory factory;

    public WhitelistAdapter(String assertionType) {
        if (assertionType == "SQL") {
            this.whitelist = new SQLWhitelistReport();
        } else if (assertionType == "DROOL  ") {
            this.whitelist = new DroolWhitelistReport();
        } else {
            this.whitelist = new MRCMWhitelistReport();
        }
    }

    public Set<WhitelistItem> detectWhitelistedItems(Set<WhitelistItem> whitelistItems) throws IOException, URISyntaxException {
        AcceptanceGatewayClient client = factory.getClient();
        List<WhitelistItem> validWhitelistedItems = client.getWhitelistedItems(whitelistItems);
        return this.whitelist.getWhitelistedItems(whitelistItems);
    }
}
