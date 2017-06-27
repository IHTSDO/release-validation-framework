package org.ihtsdo.rvf.jira;

import net.rcarz.jiraclient.JiraException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testBuildJQLString() throws JiraException {
        List<String> reportingStage = new ArrayList<>();
        reportingStage.add("Pre-Alpha");
        reportingStage.add("Alpha Feedback");
        String jql = jiraService.buildJQLForAssertionsTickets("SNOMED CT International edition", "20170731",reportingStage);
        String expectedJql = "project = ISRS AND \"SNOMED CT Product\"=\"SNOMED CT International edition\" AND \"Product Release date\"=\"20170731\" AND \"Reporting Stage\" in (\"Pre-Alpha\",\"Alpha Feedback\") AND status not in (Closed,Resolved)";
        Assert.assertEquals(expectedJql, jql);
    }

    @Test
    public void testGetProductName() throws JiraException {
        List<String> strings = jiraService.getProductNames();
        for (String string : strings) {
            System.out.println(string);
        }
    }


}
