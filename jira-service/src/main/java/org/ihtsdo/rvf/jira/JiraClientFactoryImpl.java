package org.ihtsdo.rvf.jira;

import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

/**
 * User: huyle
 * Date: 6/23/2017
 * Time: 11:30 AM
 */
public class JiraClientFactoryImpl implements JiraClientFactory{

    private ICredentials credentials;
    private String endPoint;

    public JiraClientFactoryImpl(ICredentials credentials, String endPoint) {
        this.credentials = credentials;
        this.endPoint = endPoint;
    }

    @Override
    public JiraClient getJiraClient() throws JiraException {
        return new JiraClient(this.endPoint, this.credentials);
    }
}
