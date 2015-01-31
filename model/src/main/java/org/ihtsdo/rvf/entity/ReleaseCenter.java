package org.ihtsdo.rvf.entity;

import org.ihtsdo.rvf.helper.Configuration;

import javax.persistence.*;

@Entity
@Table(name = "release_center")
public class ReleaseCenter {

	@Id
	@GeneratedValue
	private Long id;
	private String name;
	private String shortName;
	private boolean inactivated;
    @OneToOne(targetEntity = Configuration.class)
    Configuration configuration;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public boolean isInactivated() {
		return inactivated;
	}

	public void setInactivated(boolean inactivated) {
		this.inactivated = inactivated;
	}

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
