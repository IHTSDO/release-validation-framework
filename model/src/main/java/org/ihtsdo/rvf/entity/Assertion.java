package org.ihtsdo.rvf.entity;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.UUID;

/**
 * An Assertion represents a truth in snomed, it consists of a number of tests to verify
 * that assertion.
 */
@Entity
@XmlRootElement(name = "assertion")
@Table(name = "assertion")
//@JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@assertionId")
public class Assertion {

	@Id
	@GeneratedValue
    @Column(name = "assertion_id")
    private Long id;
	private String name;
	private String statement;
	private String description;
	private String docLink;
	private Date effectiveFrom;
	private String keywords;
    @Column(columnDefinition = "BINARY(16)")
    private UUID uuid = UUID.randomUUID();
//    @ManyToMany(fetch = FetchType.EAGER)
//    @JoinTable(
//            name="group_assertion_link",
//            joinColumns = @JoinColumn( name="assertion_id"),
//            inverseJoinColumns = @JoinColumn( name="group_id")
//    )
//    private Set<AssertionGroup> groups;

	public Assertion() {
	}

	public Assertion(Long id, String name) {
		this.name = name;
		this.id = id;
	}

    @XmlElement
    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    @XmlElement
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatement() {
		return statement;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}

    @XmlElement
    public String getDocLink() {
		return docLink;
	}

	public void setDocLink(String docLink) {
		this.docLink = docLink;
	}

    @XmlElement
    public Date getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

    @XmlElement
    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

    @XmlElement
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

//    public Set<AssertionGroup> getGroups() {
//        return groups;
//    }
//
//    public void setGroups(Set<AssertionGroup> groups) {
//        this.groups = groups;
//    }
//
//    /**
//     * Adds a group to this assertion. Note {@link org.ihtsdo.rvf.entity.Assertion} is always the owner of the bi-directional
//     * group-assertions link, so there should not be addAssertion method on {@link org.ihtsdo.rvf.entity.AssertionGroup}.
//     * @param group the group to be added
//     */
//    @Transient
//    public void addGroup(AssertionGroup group){
//        this.getGroups().add(group);
//        group.getAssertions().add(this);
//    }
//
//    /**
//     * Removes a group from this assertion. Note {@link org.ihtsdo.rvf.entity.Assertion} is always the owner of the bi-directional
//     * group-assertions link, so there should not be removeAssertion method on {@link org.ihtsdo.rvf.entity.AssertionGroup}.
//     * @param group the group to be added
//     */
//    @Transient
//    public void removeGroup(AssertionGroup group){
//        this.getGroups().remove(group);
//        group.getAssertions().remove(this);
//    }
}
