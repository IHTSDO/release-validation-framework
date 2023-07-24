package org.ihtsdo.rvf.core.service.structure.listing;

import org.apache.commons.digester3.binder.AbstractRulesModule;

public class ListingBuilder extends AbstractRulesModule {

	@Override
	protected void configure() {
		forPattern("listing").createObject().ofType(Listing.class.getName())
				.then().setProperties();

		forPattern("*/folder").createObject().ofType(Folder.class.getName())
				.then().addRule(new SetNamePropertyRule("Name", "folderName"))
				.then().setNext("addFolder");

		forPattern("*/file").createObject().ofType(FileElement.class.getName())
				.then().addRule(new SetNamePropertyRule("Name", "fileName"))
				.then().setNext("addFile");
	}

}
