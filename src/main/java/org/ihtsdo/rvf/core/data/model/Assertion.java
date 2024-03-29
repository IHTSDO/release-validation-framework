package org.ihtsdo.rvf.core.data.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An Assertion represents a truth in snomed, it consists of a number of tests to verify
 * that assertion.
 */

@Entity
@XmlRootElement(name = "assertion")
@Table(name = "assertion",
	indexes = @Index(columnList = "uuid"),
	uniqueConstraints = @UniqueConstraint(columnNames={"uuid"}))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Assertion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "assertion_id")
	private Long assertionId;
	
	@Column(name = "assertion_text")
	private String assertionText;
	
	private String keywords;
	
	@Column(name ="uuid")
	private String uuid = UUID.randomUUID().toString();
	
	@Column(name = "short_name")
	private String shortName;
	
	@Column(name = "doc_ref")
	private String docRef;
	
	@Column(name = "severity")
	private String severity;

	@Transient
	private String url;

	@Transient
	private String type = TestType.SQL.name();

	@Transient
	private Set<String> groups;

	public Assertion() {
	}

	public Assertion(final Long id, final String assertionText) {
		this.assertionText = assertionText;
		this.assertionId = id;
	}

	@XmlElement
	public Long getAssertionId() {
		return assertionId;
	}

	public void setAssertionId(final Long assertionId) {
		this.assertionId = assertionId;
	}

	@XmlElement
	public String getAssertionText() {
		return assertionText;
	}

	public void setAssertionText(final String assertionText) {
		this.assertionText = assertionText;
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
	
	@XmlElement
	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setGroups(Set<String> groups) {
		this.groups = groups;
	}

	public Set<String> getGroups() {
		return groups;
	}

	public void addGroup(String group) {
		if (groups == null) {
			groups = new HashSet<>();
		}
		groups.add(group);
	}

	@Override
	public String toString() {
		return "Assertion [assertionId=" + assertionId + ", assertionText=" + assertionText + ", keywords=" + keywords
				+ ", uuid=" + uuid +"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assertionId == null) ? 0 : assertionId.hashCode());
		result = prime * result
				+ ((keywords == null) ? 0 : keywords.hashCode());
		result = prime * result + ((assertionText == null) ? 0 : assertionText.hashCode());
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
		if (assertionId == null) {
			if (other.assertionId != null)
				return false;
		} else if (!assertionId.equals(other.assertionId))
			return false;
		if (keywords == null) {
			if (other.keywords != null)
				return false;
		} else if (!keywords.equals(other.keywords))
			return false;
		if (assertionText == null) {
			if (other.assertionText != null)
				return false;
		} else if (!assertionText.equals(other.assertionText))
			return false;
		if (uuid == null) {
            return other.uuid == null;
		} else return uuid.equals(other.uuid);
    }

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getDocRef() {
		return docRef;
	}

	public void setDocRef(String docRef) {
		this.docRef = docRef;
	}
}
