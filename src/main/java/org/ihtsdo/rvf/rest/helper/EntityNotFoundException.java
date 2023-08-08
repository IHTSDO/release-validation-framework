package org.ihtsdo.rvf.rest.helper;

import java.io.Serial;
import java.util.UUID;

/**
 * A custom Exception for reporting missing entities.
 *
*/
public class EntityNotFoundException extends RuntimeException{
	@Serial
	private static final long serialVersionUID = 1L;

	public EntityNotFoundException(final Long id) {
		super("No entity found with given id " + id);
	}
	public EntityNotFoundException(final UUID uuid) {
		super("No entity found with given uuid " + uuid);
	}
}
