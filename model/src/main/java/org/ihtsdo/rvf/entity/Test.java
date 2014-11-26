package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.ihtsdo.rvf.helper.Configuration;

import javax.persistence.*;

@Entity
@Table(name = "test")
public class Test {

	@Id
	@GeneratedValue
	private Long id;
	private String name;
	private String description;
    private TestType type = TestType.UNKNOWN;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "command_id")
    @JsonManagedReference
    private ExecutionCommand command;

	public Test() {
	}

	public Test(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

    public TestType getType() {
        return type;
    }

    public void setType(TestType type) {
        this.type = type;
    }

    public ExecutionCommand getCommand() {
        return command;
    }

    public void setCommand(ExecutionCommand command) {
        this.command = command;
        this.command.setTest(this);
    }

    @JsonIgnore
    public Configuration getConfiguration() {
        if(command == null){
            setCommand(new ExecutionCommand(new Configuration()));
            return command.getConfiguration();
        }
        else{
            if(command.getConfiguration() == null){
                command.setConfiguration(new Configuration());
                return command.getConfiguration();
            }
            else{
                return command.getConfiguration();
            }
        }
    }

    public void setConfiguration(Configuration configuration) {
        if(command == null){
            setCommand(new ExecutionCommand(configuration));
        }
        else{
            command.setConfiguration(configuration);
        }
    }
}
