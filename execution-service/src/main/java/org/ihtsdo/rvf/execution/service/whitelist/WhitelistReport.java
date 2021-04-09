package org.ihtsdo.rvf.execution.service.whitelist;

import java.util.Set;

public interface WhitelistReport {
    Set<WhitelistItem> getWhitelistedItems(Set<WhitelistItem> whitelistItems);
}
