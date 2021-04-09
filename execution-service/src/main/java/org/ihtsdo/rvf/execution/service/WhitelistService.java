package org.ihtsdo.rvf.execution.service;

import org.ihtsdo.rvf.execution.service.whitelist.WhitelistAdapter;
import org.ihtsdo.rvf.execution.service.whitelist.WhitelistItem;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

@Service
public class WhitelistService {
    public  Set<WhitelistItem> detectWhitelistedItems(Set<WhitelistItem> whitelistItems, String assertionType) throws IOException, URISyntaxException {
        WhitelistAdapter adapter = new WhitelistAdapter(assertionType);
        return  adapter.detectWhitelistedItems(whitelistItems);
    }
}
