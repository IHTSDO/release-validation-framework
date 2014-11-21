package org.ihtsdo.rvf.helper;


import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

/**
 * A base configuration item that can be composed to create a cong
 */
@Entity
@Table(name = "configuration_item")
public class ConfigurationItem {

    @Id
    @GeneratedValue
    Long id;
    @Column(name = "k")
    String key;
    @Column(name = "v")
    String value;
    @Column(name = "default_value")
    String defaultValue;
    boolean mandatory;
    @ManyToOne(fetch = FetchType.EAGER)
    @JsonBackReference
    @JoinColumn(name="config_id")
    Configuration configuration;

    public ConfigurationItem(String key, String value, boolean mandatory, Configuration configuration) {
        this.key = key;
        this.value = value;
        this.mandatory = mandatory;
        this.configuration = configuration;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
