package org.ihtsdo.rvf.rest.helper;

import org.ihtsdo.rvf.core.data.model.Assertion;
import org.ihtsdo.rvf.core.service.AssertionService;
import org.ihtsdo.rvf.rest.exception.InvalidFormatException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the {@code {id}} path variable the assertion REST endpoints accept,
 * which is either a UUID or a numeric database id.
 *
 * <p>Shared by {@code AssertionController} (read endpoints) and
 * {@code AssertionAdministrationController} (write endpoints), which must agree
 * on how an id is resolved. It only delegates to {@link AssertionService}.
 */
@Component
public class AssertionLookup {

	private final AssertionService assertionService;

	@Autowired
	public AssertionLookup(AssertionService assertionService) {
		this.assertionService = assertionService;
	}

	/**
	 * Attempts to look up id first as a UUID and if not, a database integer id
	 * value.
	 *
	 * @param id assertion UUID or numeric assertion id
	 * @return the referenced assertion, or {@code null} if there is none
	 */
	public Assertion find(String id) {
		if ((id == null) || id.isEmpty()) {
			throw new InvalidFormatException("Id can't be null or empty");
		}
		if (id.contains("-")) {
			try {
				UUID uuid = UUID.fromString(id);
				return assertionService.findAssertionByUUID(uuid);
			} catch (IllegalArgumentException e) {
				throw new InvalidFormatException("Id is not a valid uuid:" + id);
			}
		} else {
			try {
				return assertionService.find(Long.valueOf(id));
			} catch (IllegalArgumentException e) {
				throw new InvalidFormatException("Id is not a valid assertion id:" + id);
			}
		}
	}
}
