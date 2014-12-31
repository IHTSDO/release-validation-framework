package org.ihtsdo.rvf.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An Assertion represents a truth in snomed, it consists of a number of tests to verify
 * that assertion.
 */
@Entity
@XmlRootElement(name = "assertion")
@Table(name = "assertion")
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id", scope = Assertion.class)
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
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "assertions")
    private Set<AssertionGroup> groups = new HashSet<>();

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

    public Set<AssertionGroup> getGroups() {
        return groups;
    }

    public void setGroups(Set<AssertionGroup> groups) {
        this.groups = groups;
    }

	@Override
	public String toString() {
		return "Assertion{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}

}
