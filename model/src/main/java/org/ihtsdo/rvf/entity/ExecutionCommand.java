package org.ihtsdo.rvf.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * A class that encapsulate the logic/code that is used to run a {@link org.ihtsdo.rvf.entity.Test}.
 */
//@Embeddable
@Entity(name = "command")
@Table(name = "execution_command")
@ApiModel(description="Assertion test SQL statements")
public class ExecutionCommand {

	@Id
	@GeneratedValue
	Long id;

	@Column(columnDefinition = "text")
	@ApiModelProperty(required=false)
	String template = null;
	@JsonBackReference
	@OneToOne(fetch = FetchType.EAGER, mappedBy = "command")
	Test test;
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name="command_statements", joinColumns=@JoinColumn(name="command_id"))
	@Column(name="statement", columnDefinition = "text")
	@OrderColumn(name="statement_index")
	List<String> statements = new ArrayList<>();

	/**
	 * No argument constructor for IOC
	 */
	public ExecutionCommand() {
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(final String template) {
		this.template = template;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public Test getTest() {
		return test;
	}

	public void setTest(final Test test) {
		this.test = test;
	}

	public List<String> getStatements() {
		return statements;
	}

	public void setStatements(final List<String> statements) {
		this.statements = statements;
	}

	@Override
	public String toString() {
		return "ExecutionCommand [id=" + id + "]";
	}
}
