package org.ihtsdo.rvf.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

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
	private String keywords;
    private String uuid = UUID.randomUUID().toString();

    public Assertion() {
	}

	public Assertion(final Long id, final String name) {
		this.name = name;
		this.id = id;
	}

    @XmlElement
    public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

    @XmlElement
    public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(final String keywords) {
		this.keywords = keywords;
	}

    @XmlElement
    public UUID getUuid() {
        return UUID.fromString(uuid);
    }

    public void setUuid(final UUID uuid) {
        this.uuid = uuid.toString();
    }

	@Override
	public String toString() {
		return "Assertion{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((keywords == null) ? 0 : keywords.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Assertion other = (Assertion) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (keywords == null) {
			if (other.keywords != null)
				return false;
		} else if (!keywords.equals(other.keywords))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
}
