package org.ihtsdo.rvf.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An entity that represents a collection of {@link org.ihtsdo.rvf.entity.Assertion}s. Note that this is deliberately not
 * a recursive collection of {@link org.ihtsdo.rvf.entity.Assertion}s.
 */
@Entity
@Table(name = "assertion_group")
public class AssertionGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "group_id")
	private Long id;
	
	private String name;
	
	@ManyToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(
			name="group_assertion_link",
			joinColumns = @JoinColumn( name="group_id"),
			inverseJoinColumns = @JoinColumn( name="assertion_id")
	)
	@JsonIgnore
	private Set<Assertion> assertions = new HashSet<>();
	
	@Transient
	private final int total = 0;
	
	
	public int getTotal() {
		return assertions.size();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Set<Assertion> getAssertions() {
		return assertions;
	}

	public void setAssertions(final Set<Assertion> assertions) {
		this.assertions = assertions;
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	/**
	 * Adds an assertion to this group. Note {@link org.ihtsdo.rvf.entity.AssertionGroup} is always the owner of the bi-directional
	 * group-assertions link, so there should not be addAssertion method on {@link org.ihtsdo.rvf.entity.Assertion}.
	 * @param assertion the assertion to be added
	 */
	@Transient
	public void addAssertion(final Assertion assertion){
		this.getAssertions().add(assertion);
	}

	/**
	 * Removes a assertion from this group. Note {@link org.ihtsdo.rvf.entity.AssertionGroup} is always the owner of the bi-directional
	 * group-assertions link, so there should not be removeAssertion method on {@link org.ihtsdo.rvf.entity.Assertion}.
	 * @param assertion the assertion to be removed
	 */
	@Transient
	public void removeAssertion(final Assertion assertion) {
		this.getAssertions().remove(assertion);
	}
	
	public void removeAllAssertionsFromGroup() {
		assertions.clear();
	}

	@Override
	public String toString() {
		return "AssertionGroup [id=" + id + ", name=" + name + ", total assertions =" + assertions.size() + "]";
	}
}
