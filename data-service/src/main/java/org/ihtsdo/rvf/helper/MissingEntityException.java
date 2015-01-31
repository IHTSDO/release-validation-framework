package org.ihtsdo.rvf.helper;

import java.util.UUID;

/**
 * A custom Exception for reporting missing entities.
 *
 */
public class MissingEntityException extends RuntimeException{

    public MissingEntityException(final Long id) {
        super("No entity found with given id " + id);
    }
    public MissingEntityException(final UUID uuid) {
        super("No entity found with given uuid " + uuid);
    }
}
