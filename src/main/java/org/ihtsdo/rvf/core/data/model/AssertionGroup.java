package org.ihtsdo.rvf.core.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * An entity that represents a collection of {@link Assertion}s. Note that this is deliberately not
 * a recursive collection of {@link Assertion}s.
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
	 * Adds an assertion to this group. Note {@link AssertionGroup} is always the owner of the bi-directional
	 * group-assertions link, so there should not be addAssertion method on {@link Assertion}.
	 * @param assertion the assertion to be added
	 */
	@Transient
	public void addAssertion(final Assertion assertion){
		this.getAssertions().add(assertion);
	}

	/**
	 * Removes a assertion from this group. Note {@link AssertionGroup} is always the owner of the bi-directional
	 * group-assertions link, so there should not be removeAssertion method on {@link Assertion}.
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
