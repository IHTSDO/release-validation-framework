package org.ihtsdo.rvf.helper;

/**
 * A custom Exception for reporting missing entities.
 *
 */
public class MissingEntityException extends RuntimeException{

    public MissingEntityException(final Long id) {
        super("No entity found with given id " + id);
    }
}
