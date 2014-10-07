package org.ihtsdo.rvf.entity;

import com.google.common.base.Preconditions;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that encapsulate the logic/code that is used to run a {@link org.ihtsdo.rvf.entity.Test}.
 */
@Embeddable
@Entity
public class ExecutionCommand {

    @Id
    @GeneratedValue
    Long id;

    @ElementCollection(fetch = FetchType.LAZY)
    @MapKeyColumn(name="key")
    @Column(name="value")
    @CollectionTable(name="command_parameters", joinColumns=@JoinColumn(name="command_id"))
    Map<String, String> parameters = new HashMap<>();
    String template = null;
    @Column(columnDefinition = "BLOB")
    byte[] code = null;

    public ExecutionCommand(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * No argument constructor for IOC
     */
    public ExecutionCommand() {
    }

    public void validate(TestType type) throws Exception{

        Preconditions.checkNotNull(type, "Test type passed can not be null");
        // do something there to validate the template against the template
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
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
