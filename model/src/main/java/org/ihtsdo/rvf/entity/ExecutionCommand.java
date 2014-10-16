package org.ihtsdo.rvf.entity;

import org.ihtsdo.rvf.helper.Configuration;

import javax.persistence.*;

/**
 * A class that encapsulate the logic/code that is used to run a {@link org.ihtsdo.rvf.entity.Test}.
 */
@Embeddable
@Entity
public class ExecutionCommand {

    @Id
    @GeneratedValue
    Long id;
    @OneToOne(targetEntity = Configuration.class)
    Configuration configuration;
    String template = null;
    @Transient
    byte[] code = null;

    public ExecutionCommand(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * No argument constructor for IOC
     */
    public ExecutionCommand() {
    }

    public boolean validate(TestType type, Configuration testConfiguration){

        boolean valid = true;
        if(type == null){
            return false;
        }
        // get all instances of keys in configuration and see if they have all been set
        for(String key : configuration.getKeys())
        {
            if(testConfiguration.getValue(key) == null)
            {
                valid = false;
                break;
            }
        }

        return valid;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public byte[] getCode() {
        return code;
    }

    public void setCode(byte[] code) {
        this.code = code;
    }
}
