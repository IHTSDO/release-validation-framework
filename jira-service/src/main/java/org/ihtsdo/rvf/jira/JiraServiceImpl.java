package org.ihtsdo.rvf.jira;

import net.rcarz.jiraclient.CustomFieldOption;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * User: huyle
 * Date: 6/22/2017
 * Time: 4:56 PM
 */
@Service
public class JiraServiceImpl implements JiraService{

    @Autowired
    private JiraClientFactory jiraClientFactory;

    @Value("${rvf.jira.field.productName}")
    private String fieldProductName;

    @Value("${rvf.jira.field.id.productName}")
    private String fieldIdProductName;

    @Value("${rvf.jira.field.releaseDate}")
    private String fieldReleaseDate;

    @Value("${rvf.jira.field.reportingStage}")
    private String fieldReportStage;

    @Value("${rvf.jira.field.id.reportingStage}")
    private String fieldIdReportStage;

    @Value("${rvf.jira.value.projectKey}")
    private String valueProjectKey;

    @Value("${rvf.jira.value.resolvedStatus}")
    private String valueResolvedStatuses;


    @Override
    public List<Issue> getTicketsByJQL(String jql) throws JiraException {
        JiraClient jiraClient = jiraClientFactory.getJiraClient();
        Issue.SearchResult searchResult = jiraClient.searchIssues(jql);
        return searchResult.issues;
    }

    @Override
    public String buildJQLForAssertionsTickets(String productName, String releaseDate, List<String> prevReportingStages) throws JiraException {
        StringBuilder jqlBuilder= new StringBuilder();
        jqlBuilder.append("project = ").append(valueProjectKey).append(" AND ");
        jqlBuilder.append("\"" + fieldProductName + "\"").append("=").append("\""+productName+"\"").append(" AND ");
        jqlBuilder.append("\"" + fieldReleaseDate  + "\"").append("=").append("\""+ releaseDate + "\"").append(" AND ");
        if(prevReportingStages != null && !prevReportingStages.isEmpty()) {
            StringBuilder reportingStagesBuilder = new StringBuilder();
            for (String stage : prevReportingStages) {
                reportingStagesBuilder.append("\"" + stage + "\"").append(",");
            }
            reportingStagesBuilder.deleteCharAt(reportingStagesBuilder.length()-1);
            jqlBuilder.append("\"" + fieldReportStage  + "\"").append(" in (").append(reportingStagesBuilder.toString()).append(") AND ");
        }
        jqlBuilder.append("status not in ").append("("+valueResolvedStatuses+")");
        return jqlBuilder.toString();
    }

    @Override
    public List<String> getAllowedValuesForCustomField(String customField) throws JiraException {
        JiraClient jiraClient = jiraClientFactory.getJiraClient();
        List<CustomFieldOption> customFieldOptions = jiraClient.getCustomFieldAllowedValues(customField, valueProjectKey, "Bug");
        List<String> allowedValues = new ArrayList<>();
        for (CustomFieldOption customFieldOption : customFieldOptions) {
            allowedValues.add(customFieldOption.getValue());
        }
        return allowedValues;
    }

    @Override
    public List<String> getReportingStages() throws JiraException {
        return getAllowedValuesForCustomField(fieldIdReportStage);
    }

    @Override
    public List<String> getProductNames() throws JiraException {
        return getAllowedValuesForCustomField(fieldIdProductName);
    }

    @Override
    public List<Issue> getCurrentProductFailureTickets(String productName, String releaseDate, String currentReportingStage) throws JiraException {
        boolean validProductName = false;
        boolean validReportingStage = false;
        List<Issue> issues = null;
        List<String> productNames = getProductNames();
        for (String name : productNames) {
            if(name.equalsIgnoreCase(productName)) {
                validProductName = true;
                break;
            }
        }
        List<String> reportingStages = getReportingStages();
        List<String> previousReportingStages = new ArrayList<>();
        for (String name : reportingStages) {
            if(name.equalsIgnoreCase(currentReportingStage)) {
                validReportingStage = true;
                break;
            } else {
                previousReportingStages.add(name);
            }
        }
        if(validProductName && validReportingStage) {
            String jql = buildJQLForAssertionsTickets(productName, releaseDate, previousReportingStages);
            issues = getTicketsByJQL(jql);
        }
        return issues;
    }
}
