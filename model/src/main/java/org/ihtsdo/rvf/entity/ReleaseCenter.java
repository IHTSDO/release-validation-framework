package org.ihtsdo.rvf.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "release_center")
public class ReleaseCenter {

	@Id
	@GeneratedValue
	private Long id;
	private String name;
	private String shortName;
	private boolean inactivated;

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(final String shortName) {
		this.shortName = shortName;
	}

	public boolean isInactivated() {
		return inactivated;
	}

	public void setInactivated(final boolean inactivated) {
		this.inactivated = inactivated;
	}
}
