package org.ihtsdo.rvf.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Entity
@Table(name = "test")
@ApiModel( description="Test")
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

	public Test(final Long id, final String name) {
		this.id = id;
		this.name = name;
	}

	@ApiModelProperty(value="Auto generated test Id", required=false) 
	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	@ApiModelProperty(value="Test name", required=false)
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@ApiModelProperty(value="Test description", required=false)
	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	@ApiModelProperty(value="Test type", required=true)
	public TestType getType() {
		return type;
	}

	public void setType(final TestType type) {
		this.type = type;
	}

	@ApiModelProperty(value="Test execution command", required=true)
	public ExecutionCommand getCommand() {
		return command;
	}

	public void setCommand(final ExecutionCommand command) {
		this.command = command;
		this.command.setTest(this);
	}

	@Override
	public String toString() {
		return "Test [id=" + id + ", name=" + name + ", description="
				+ description + ", type=" + type + "]";
	}
}
