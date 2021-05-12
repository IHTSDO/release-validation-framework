package org.ihtsdo.rvf.execution.service.whitelist;

import java.util.Objects;

public class WhitelistItem {
    private String validationRuleId;

    private String componentId;

    private String conceptId;

    private String branch;

    private String additionalFields;

    public WhitelistItem (String validationRuleId, String componentId, String conceptId, String additionalFields) {
        this.validationRuleId = validationRuleId;
        this.conceptId = conceptId;
        this.componentId = componentId;
        this.additionalFields = additionalFields;
    }

    public String getValidationRuleId() {
        return validationRuleId;
    }

    public void setValidationRuleId(String validationRuleId) {
        this.validationRuleId = validationRuleId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(String additionalFields) {
        this.additionalFields = additionalFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhitelistItem that = (WhitelistItem) o;
        return Objects.equals(validationRuleId, that.validationRuleId) &&
                Objects.equals(componentId, that.componentId) &&
                Objects.equals(conceptId, that.conceptId) &&
                Objects.equals(branch, that.branch) &&
                Objects.equals(additionalFields, that.additionalFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validationRuleId, componentId, conceptId, branch, additionalFields);
    }
}
