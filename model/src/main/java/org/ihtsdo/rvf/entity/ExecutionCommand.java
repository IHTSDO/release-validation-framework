package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.ihtsdo.rvf.helper.Configuration;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that encapsulate the logic/code that is used to run a {@link org.ihtsdo.rvf.entity.Test}.
 */
//@Embeddable
@Entity(name = "command")
@Table(name = "execution_command")
public class ExecutionCommand {

    @Id
    @GeneratedValue
    Long id;
    @JsonManagedReference
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "configuration_id")
    Configuration configuration;
    @Column(columnDefinition = "text")
    String template = null;
    @Transient
    byte[] code = null;
    @JsonBackReference
    @OneToOne(fetch = FetchType.EAGER, mappedBy = "command")
    Test test;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="command_statements", joinColumns=@JoinColumn(name="command_id"))
    @Column(name="statement", columnDefinition = "text")
    @OrderColumn(name="statement_index")
    List<String> statements = new ArrayList<>();

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
        this.configuration.setCommand(this);
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public List<String> getStatements() {
        return statements;
    }

    public void setStatements(List<String> statements) {
        this.statements = statements;
    }
}
