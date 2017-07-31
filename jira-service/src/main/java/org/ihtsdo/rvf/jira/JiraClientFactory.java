package org.ihtsdo.rvf.jira;

import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

/**
 * User: huyle
 * Date: 6/23/2017
 * Time: 11:29 AM
 */
public interface JiraClientFactory {

    JiraClient getJiraClient() throws JiraException;
}