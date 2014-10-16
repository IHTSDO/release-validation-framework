package org.ihtsdo.rvf.helper;

import javax.persistence.Embeddable;

/**
 * A base configuration item that can be composed to create a cong
 */
@Embeddable
//@Entity
public class ConfigurationItem {

    String key;
    String value;
    String defaultValue;
    boolean mandatory;

    public ConfigurationItem(String key, String value, boolean mandatory) {
        this.key = key;
        this.value = value;
        this.mandatory = mandatory;
    }

    public ConfigurationItem() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
