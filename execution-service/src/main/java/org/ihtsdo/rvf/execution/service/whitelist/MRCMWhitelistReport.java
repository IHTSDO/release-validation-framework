package org.ihtsdo.rvf.execution.service.whitelist;

import java.util.Collections;
import java.util.Set;

public class MRCMWhitelistReport implements WhitelistReport {
    @Override
    public Set<WhitelistItem> getWhitelistedItems(Set<WhitelistItem> whitelistItems) {
        return Collections.emptySet();
    }
}
