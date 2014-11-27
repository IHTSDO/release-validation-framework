package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * An entity that reprsents a collection of {@link org.ihtsdo.rvf.entity.Assertion}s. Note that this is deliberately not
 * a recursive collection of {@link org.ihtsdo.rvf.entity.Assertion}s.
 */
@Entity
@Table(name = "assertion_group")
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class AssertionGroup {

    @Id
    @GeneratedValue
    @Column(name = "group_id")
    Long id;
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name="group_assertion_link",
            joinColumns = @JoinColumn( name="group_id"),
            inverseJoinColumns = @JoinColumn( name="assertion_id")
    )
    Set<Assertion> assertions = new HashSet<>();
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Assertion> getAssertions() {
        return assertions;
    }

    public void setAssertions(Set<Assertion> assertions) {
        this.assertions = assertions;
        for(Assertion assertion : this.assertions){
            assertion.getGroups().add(this);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Adds an assertion to this gruop. Note {@link org.ihtsdo.rvf.entity.AssertionGroup} is always the owner of the bi-directional
     * group-assertions link, so there should not be addAssertion method on {@link org.ihtsdo.rvf.entity.Assertion}.
     * @param assertion the assertion to be added
     */
    @Transient
    public void addAssertion(Assertion assertion){
        this.getAssertions().add(assertion);
        assertion.getGroups().add(this);
    }

    /**
     * Removes a assertion from this group. Note {@link org.ihtsdo.rvf.entity.AssertionGroup} is always the owner of the bi-directional
     * group-assertions link, so there should not be removeAssertion method on {@link org.ihtsdo.rvf.entity.Assertion}.
     * @param assertion the assertion to be removed
     */
    @Transient
    public void removeAssertion(Assertion assertion){
        this.getAssertions().remove(assertion);
        assertion.getGroups().remove(this);
    }
}
