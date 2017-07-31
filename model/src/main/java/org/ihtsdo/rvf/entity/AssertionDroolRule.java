package org.ihtsdo.rvf.entity;

import org.ihtsdo.drools.response.InvalidContent;

import java.util.List;

/**
 * Created by Tin Le
 * on 6/28/2017.
 */
public class AssertionDroolRule {
        private String rule;
        private int totalFails;
        private List<InvalidContent> contentItems;

        public String getrule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
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
