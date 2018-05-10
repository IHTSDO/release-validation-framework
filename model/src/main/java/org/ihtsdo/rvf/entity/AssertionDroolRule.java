package org.ihtsdo.rvf.entity;

import org.ihtsdo.drools.response.InvalidContent;

import java.util.List;

/**
 * Created by Tin Le
 * on 6/28/2017.
 */
public class AssertionDroolRule {
        private String groupRule;
        private int totalFails;
        private List<InvalidContent> contentItems;

        public String getGroupRule() {
            return groupRule;
        }

        public void setGroupRule(String groupRule) {
            this.groupRule = groupRule;
        }

        public List<InvalidContent> getContentItems() {
            return contentItems;
        }

        public void setContentItems(List<InvalidContent> contentItems) {
            this.contentItems = contentItems;
        }

        public int getTotalFails() {
            return totalFails;
        }

        public void setTotalFails(int totalFails) {
            this.totalFails = totalFails;
        }
}
