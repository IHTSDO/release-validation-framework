package org.ihtsdo.rvf.jira;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

import java.util.List;

/**
 * User: huyle
 * Date: 6/22/2017
 * Time: 4:02 PM
 */
public interface JiraService {

    public List<Issue> getTicketsByJQL(String jql) throws JiraException;

    public String buildJQLForAssertionsTickets(String productName, String releaseDate, List<String> prevReportingStages) throws JiraException;

    public List<String> getAllowedValuesForCustomField(String customField) throws JiraException;

    public List<String> getReportingStages() throws JiraException;

    public List<String> getProductNames() throws JiraException;

    public List<Issue> getCurrentProductFailureTickets(String productName, String releaseDate, String currentReportingStage) throws JiraException;

    public Issue createRVFFailureTicket(String summary, String description, String productName, String releaseDate, String reportingStage) throws JiraException;

    public void addJiraTickets(String productName, String releaseDate, String currentReportingStage, Object failedItem) throws JiraException;

}
