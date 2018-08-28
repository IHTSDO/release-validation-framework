package org.ihtsdo.rvf.helper;

import java.util.UUID;

/**
 * A custom Exception for reporting missing entities.
 *
 */
public class EntityNotFoundException extends RuntimeException{
	private static final long serialVersionUID = 1L;

	public EntityNotFoundException(final Long id) {
		super("No entity found with given id " + id);
	}
	public EntityNotFoundException(final UUID uuid) {
		super("No entity found with given uuid " + uuid);
	}
}
