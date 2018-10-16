package org.ihtsdo.rvf.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


@Entity
@Table(name = "qa_result")
public class QAResult implements Serializable {

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
	
	@Lob
	@Column(name ="details")
	private  String details;

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
	
}
