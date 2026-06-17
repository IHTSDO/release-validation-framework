package org.ihtsdo.rvf.core.data.model;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;


@Entity
@Table(name = "qa_result")
public class QAResult implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	
	@Column(name = "run_id")
	private Long runId;

	@JoinColumn(name ="assertion_id")
	@ManyToOne
	private Assertion assertion;
	
	@Column(name = "concept_id")
	private Long conceptId;

	@Column(name ="details", columnDefinition = "TEXT")
	private String details;

	@Column(name = "component_id")
	private String componentId;

	@Column(name ="table_name")
	private String tableName;

	@Column(name ="skip_module_check")
	private Boolean skipModuleCheck = false;

	public String getDetails() {
		return details;
	}

	public void setDetails(final String details) {
		this.details = details;
	}

	public Long getRunId() {
		return runId;
	}

	public void setRunId(final Long runId) {
		this.runId = runId;
	}

	public Assertion getAssertion() {
		return assertion;
	}

	public void setAssertion(final Assertion assertion) {
		this.assertion = assertion;
	}

	public Long getConceptId() {
		return conceptId;
	}

	public void setConceptId(Long conceptId) {
		this.conceptId = conceptId;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Boolean getSkipModuleCheck() {
		return skipModuleCheck;
	}

	public void setSkipModuleCheck(Boolean skipModuleCheck) {
		this.skipModuleCheck = skipModuleCheck;
	}
}
