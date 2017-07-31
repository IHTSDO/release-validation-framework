package org.ihtsdo.rvf.jira;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraException;

/**
 * User: huyle
 * Date: 6/23/2017
 * Time: 3:17 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath*:testJiraServiceContext.xml")
public class JiraServiceImplTest {

    @Autowired
    private JiraService jiraService;

    //@Test
    public void testBuildJQLString() throws JiraException {
        List<String> reportingStage = new ArrayList<>();
        reportingStage.add("Pre-Alpha");
        reportingStage.add("Alpha Feedback");
        String jql = jiraService.buildJQLForAssertionsTickets("SNOMED CT International edition", "20170731",reportingStage);
        String expectedJql = "project = ISRS AND \"SNOMED CT Product\"=\"SNOMED CT International edition\" AND \"Product Release date\"=\"20170731\" AND \"Reporting Stage\" in (\"Pre-Alpha\",\"Alpha Feedback\") AND status not in (Closed,Resolved)";
        Assert.assertEquals(expectedJql, jql);
    }
    
    @Test
    public void testGetProductNames() throws JiraException {
    	List<String> prodNames = jiraService.getProductNames();
    	Assert.assertTrue(prodNames.size() > 0);
    }
    
    //@Test
    public void testGetReportingStages() throws JiraException {
    	List<String> prodNames = jiraService.getReportingStages();
    	Assert.assertTrue(prodNames.size() > 0);
    }
    
    //@Test
    public void testCreatJiraTicket() throws JiraException {
    	Issue newIssue = jiraService.createRVFFailureTicket("Quyen Ly testing 30", "description", "SNOMED CT International edition", "2017-07-31", "Pre-Alpha");
    	Assert.assertTrue(!newIssue.getUrl().isEmpty());
    }
    
    
}
