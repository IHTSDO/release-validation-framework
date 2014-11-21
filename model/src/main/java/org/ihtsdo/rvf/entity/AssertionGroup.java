package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;

/**
 * An entity that reprsents a collection of {@link org.ihtsdo.rvf.entity.Assertion}s. Note that this is deliberately not
 * a recursive collection of {@link org.ihtsdo.rvf.entity.Assertion}s.
 */
@Entity
@Table(name = "assertion_group")
@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@groupId")
public class AssertionGroup {

    @Id
    @GeneratedValue
    @Column(name = "group_id")
    Long id;
//    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "groups")
//    Set<Assertion> assertions;
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public Set<Assertion> getAssertions() {
//        return assertions;
//    }
//
//    public void setAssertions(Set<Assertion> assertions) {
//        this.assertions = assertions;
//    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
