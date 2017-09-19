package org.ihtsdo.rvf.jira;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.rcarz.jiraclient.*;
import org.ihtsdo.rvf.entity.FailureDetail;
import org.ihtsdo.rvf.entity.TestRunItem;
import org.json.JSONObject;
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
public class JiraServiceImpl implements JiraService {

    @Autowired
    private JiraClientFactory jiraClientFactory;

    @Value("${rvf.jira.field.productName}")
    private String fieldProductName;

    @Value("${rvf.jira.field.id.productName}")
    private String fieldIdProductName;

    @Value("${rvf.jira.field.releaseDate}")
    private String fieldReleaseDate;

    @Value("${rvf.jira.field.id.releaseDate}")
    private String fieldIdReleaseDate;

    @Value("${rvf.jira.field.reportingStage}")
    private String fieldReportStage;

    @Value("${rvf.jira.field.id.reportingStage}")
    private String fieldIdReportStage;

    @Value("${rvf.jira.value.projectKey}")
    private String valueProjectKey;

    @Value("${rvf.jira.value.resolvedStatus}")
    private String valueResolvedStatuses;

    @Value("${rvf.jira.defaultAssignee}")
    private String defaultAssignee;

    @Value("${rvf.jira.endpoint}")
    private String jiraEndpoint;

    @Override
    public List<Issue> getTicketsByJQL(String jql) throws JiraException {
        JiraClient jiraClient = jiraClientFactory.getJiraClient();
        Issue.SearchResult searchResult = jiraClient.searchIssues(jql, 2000);
        return searchResult.issues;
    }

    @Override
    public String buildJQLForAssertionsTickets(String productName, String releaseDate, List<String> prevReportingStages) throws JiraException {
        StringBuilder jqlBuilder = new StringBuilder();
        jqlBuilder.append("project = ").append(valueProjectKey).append(" AND ");
        jqlBuilder.append("\"" + fieldProductName + "\"").append("=").append("\"" + productName + "\"").append(" AND ");
        jqlBuilder.append("\"" + fieldReleaseDate + "\"").append("=").append("\"" + releaseDate + "\"").append(" AND ");
        if (prevReportingStages != null && !prevReportingStages.isEmpty()) {
            StringBuilder reportingStagesBuilder = new StringBuilder();
            for (String stage : prevReportingStages) {
                reportingStagesBuilder.append("\"" + stage + "\"").append(",");
            }
            reportingStagesBuilder.deleteCharAt(reportingStagesBuilder.length() - 1);
            jqlBuilder.append("\"" + fieldReportStage + "\"").append(" in (").append(reportingStagesBuilder.toString()).append(") AND ");
        }
        jqlBuilder.append("status not in ").append("(" + valueResolvedStatuses + ")");
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
            if (name.equalsIgnoreCase(productName)) {
                validProductName = true;
                break;
            }
        }
        List<String> reportingStages = getReportingStages();
        List<String> previousReportingStages = new ArrayList<>();
        for (String name : reportingStages) {
            previousReportingStages.add(name);
            if (name.equalsIgnoreCase(currentReportingStage)) {
                validReportingStage = true;
                break;
            }
        }
        if (validProductName && validReportingStage) {
            String jql = buildJQLForAssertionsTickets(productName, releaseDate, previousReportingStages);
            issues = getTicketsByJQL(jql);
        }
        return issues;
    }

    @Override
    public Issue createRVFFailureTicket(String summary, String description, String productName, String releaseDate, String reportingStage) throws JiraException {
        JiraClient jiraClient = jiraClientFactory.getJiraClient();
        if (summary.length() > 255) summary = summary.substring(0, 252) + "...";
        JSONObject productNameObj = new JSONObject();
        productNameObj.put("value", productName);
        JSONObject reportingStageObj = new JSONObject();
        reportingStageObj.put("value", reportingStage);
        ArrayList products = new ArrayList();
        products.add(productName);
        ArrayList reportingStages = new ArrayList();
        reportingStages.add(reportingStage);
        Issue issue = jiraClient.createIssue(valueProjectKey, "Bug")
                .field(Field.SUMMARY, summary)
                .field(Field.DESCRIPTION, description)
                .field(fieldIdProductName, productNameObj)
                .field(fieldIdReportStage, reportingStages)
                .field(fieldIdReleaseDate, releaseDate)
                .execute();
        if (!defaultAssignee.isEmpty()) {
            issue.update()
                    .field(Field.ASSIGNEE, defaultAssignee)
                    .execute();
        }
        return issue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addJiraTickets(String productName, String releaseDate, String currentReportingStage,
                               Object failedItem) throws JiraException {
        List<Issue> issues = getCurrentProductFailureTickets(productName, releaseDate, currentReportingStage);
        List<TestRunItem> items = (List<TestRunItem>) failedItem;
        if (issues == null || issues.isEmpty()) {
            for (TestRunItem item : items) {
                createJiraTicket(productName, releaseDate, currentReportingStage, item);
            }
        } else {
            for (TestRunItem item : items) {
                boolean itemFound = false;
                for (Issue issue : issues) {
                    // If Issue was found, then return URL of issue
                    if (item.getAssertionUuid() != null && issue.getSummary().contains(item.getAssertionUuid().toString())
                            && item.getFailureCount().longValue() == getIssueFailureCount(issue)) {
                        String url = getBrowseURL(issue);
                        item.setJiraLink(url);
                        itemFound = true;
                        break;
                    }
                }
                if (!itemFound) {
                    createJiraTicket(productName, releaseDate, currentReportingStage, item);
                }
            }
        }
    }

    private long getIssueFailureCount(Issue issue) {
        String desc = issue.getDescription();
        String failureCountText = "failureCount";
        int startPositionFailureCount = desc.indexOf(failureCountText);
        if (startPositionFailureCount > 0) {
            startPositionFailureCount = startPositionFailureCount + failureCountText.length() + 4;
            int endPositionFailureCount = desc.substring(startPositionFailureCount, startPositionFailureCount + 20).indexOf(",");
            return Long.parseLong(desc.substring(startPositionFailureCount, startPositionFailureCount + endPositionFailureCount).trim());
        }
        return 0;
    }

    private void createJiraTicket(String productName, String releaseDate, String currentReportingStage, TestRunItem item)
            throws JiraException {
        try {
            String summary = getSummary(item);
            String descJSON;
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.setSerializationInclusion(Include.NON_NULL);
            objectMapper.setSerializationInclusion(Include.NON_EMPTY);
            ObjectWriter writer = objectMapper.writer();

            descJSON = writer.writeValueAsString(item);
            descJSON = descJSON.substring(1, descJSON.length() - 1); // Remove first and end bracket
            String description = descJSON.length() > 10000 ? descJSON.substring(0, 10000) + " ..." : descJSON;
            Issue newIssue = createRVFFailureTicket(summary, description, productName, releaseDate, currentReportingStage);
            item.setJiraLink(getBrowseURL(newIssue));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private String getSummary(TestRunItem item) {
        String summary = "";
        if(item.getTestType() != null){
            summary += "[" + item.getTestType().toString().toUpperCase() + "] ";
        }
        summary += "Failed assertion: ";
        if(item.getAssertionUuid() != null) {
            summary += item.getAssertionUuid().toString() + ". ";
        }

        switch (item.getTestType()){
            case SQL:
            case MRCM:
            case DROOL_RULES:
                summary += item.getAssertionText();
                break;
            case ARCHIVE_STRUCTURAL:
                if(item.getFirstNInstances() != null && item.getFirstNInstances().size() > 0) {
                    for (FailureDetail failItem : item.getFirstNInstances()) {
                        summary += failItem.getDetail() + ". ";
                    }
                }
                break;
        }
        return summary;
    }

    private String getBrowseURL(Issue issue) {
        return jiraEndpoint + "/browse/" + issue.getKey();
    }
}
