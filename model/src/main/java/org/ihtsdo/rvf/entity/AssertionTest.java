package org.ihtsdo.rvf.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Class represents the association between an assertion and a test, here we determine whether the test is active
 * for the given assertion without inactivating the test, just for the given assertion for the given releaseCenter,
 * this enables Tests to remain in the pool for attachment to other assertions and centers. Class is used to pass
 * information to the build system to output the poms used to execute tests.
 */

@Entity
@Table(name = "assertion_test")
public class AssertionTest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(cascade = CascadeType.PERSIST)
	private Test test;

	@ManyToOne(cascade = CascadeType.PERSIST)
	private Assertion assertion;

	private boolean inactive;

	public AssertionTest() {
	}

	public AssertionTest(final Long id, final String name) {
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

	public Assertion getAssertion() {
		return assertion;
	}

	public void setAssertion(final Assertion assertion) {
		this.assertion = assertion;
	}


	public boolean isInactive() {
		return inactive;
	}

	@Override
	public String toString() {
		return "AssertionTest [id=" + id + ", test=" + test + ", assertion="
				+ assertion + ", inactive=" + inactive + "]";
	}

	public void setInactive(final boolean inactive) {
		this.inactive = inactive;
	}

}
