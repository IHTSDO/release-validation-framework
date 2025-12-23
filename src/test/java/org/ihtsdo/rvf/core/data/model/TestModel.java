package org.ihtsdo.rvf.core.data.model;

/*
 * Test is a reserved keyword in unit tests. Importing the full package name resolves the issue but clutters the code. This class
 * saves the need to do it, i.e., TestModel instead of org.ihtsdo.rvf.core.data.model.Test.
 * */
public class TestModel extends Test {
	private Long id;
	private String name;
	private String description;
	private TestType type = TestType.UNKNOWN;
	private ExecutionCommand command;

	public TestModel() {
	}

	public TestModel(Test test) {
		if (test == null) {
			return;
		}

		this.id = test.getId();
		this.name = test.getName();
		this.description = test.getDescription();
		this.type = test.getType();
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public TestType getType() {
		return type;
	}

	@Override
	public void setType(TestType type) {
		this.type = type;
	}

	@Override
	public ExecutionCommand getCommand() {
		return command;
	}

	@Override
	public void setCommand(ExecutionCommand command) {
		this.command = command;
	}
}
