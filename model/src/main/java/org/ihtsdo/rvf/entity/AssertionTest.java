package org.ihtsdo.rvf.entity;

import javax.persistence.*;

/**
 * Class represents the association between an assertion and a test, here we determine whether the test is active
 * for the given assertion without inactivating the test, just for the given assertion for the given releaseCenter,
 * this enables Tests to remain in the pool for attachment to other assertions and centers. Class is used to pass
 * information to the build system to output the poms used to execute tests.
 */

@Entity
public class AssertionTest {

    public AssertionTest() {
    }

    public AssertionTest(Long id, String name) {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public void setAssertion(Assertion assertion) {
        this.assertion = assertion;
    }

    public ReleaseCenter getCenter() {
        return center;
    }

    public void setCenter(ReleaseCenter center) {
        this.center = center;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Test test;

    @ManyToOne
    private Assertion assertion;

    @ManyToOne
    private ReleaseCenter center;

    private boolean inactive;

}
