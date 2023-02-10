package org.ihtsdo.rvf.core.data.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that encapsulate the logic/code that is used to run a {@link Test}.
 */
@Embeddable
@Entity(name = "command")
@Table(name = "execution_command")
public class ExecutionCommand {

	@Id
	@GeneratedValue
	Long id;

	@Column(columnDefinition = "text")
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
