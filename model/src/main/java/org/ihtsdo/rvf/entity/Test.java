package org.ihtsdo.rvf.entity;

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
    @OneToOne(targetEntity = Configuration.class)
    Configuration configuration;
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity = ExecutionCommand.class)
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
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
