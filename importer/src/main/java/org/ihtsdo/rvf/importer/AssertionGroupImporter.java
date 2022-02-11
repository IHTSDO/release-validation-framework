package org.ihtsdo.rvf.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.ihtsdo.rvf.entity.Assertion;
import org.ihtsdo.rvf.entity.AssertionGroup;
import org.ihtsdo.rvf.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.ihtsdo.rvf.importer.AssertionGroupImporter.AssertionGroupName.*;

@Service
@Transactional
public class AssertionGroupImporter {

	private static final String PREVIOUS = "previous";
	private static final String NEW_INACTIVE_STATES_FOLLOW_ACTIVE_STATES = "New inactive states follow active states";
	public static final String MDRS = "mdrs";

	enum AssertionGroupName {
		FILE_CENTRIC_VALIDATION ("COMMON", "file-centric-validation"),
		COMPONENT_CENTRIC_VALIDATION ("COMMON", "component-centric-validation"),
		RELEASE_TYPE_VALIDATION ("COMMON", "release-type-validation"),
		MDRS_VALIDATION ("mdrs", "mdrs"),
		SPANISH_EDITION ("ES", "SpanishEdition"),
		INTERNATIONAL_EDITION ("INT", "InternationalEdition"),
		COMMON_AUTHORING ("COMMON", "common-authoring"),
		COMMON_AUTHORING_WITHOUT_LANG_REFSETS ("COMMON_AUTHORING_WITHOUT_LANG_REFSETS", "common-authoring-without-lang-refsets"),
		COMMON_EDITION("COMMON", "common-edition"),
		INT_AUTHORING ("INT", "int-authoring"),
		AT_AUTHORING("AT", "at-authoring"),
		DK_AUTHORING("DK", "dk-authoring"),
		SE_AUTHORING ("SE", "se-authoring"),
		US_AUTHORING ("US", "us-authoring"),
		BE_AUTHORING("BE", "be-authoring"),
		NO_AUTHORING("NO", "no-authoring"),
		CH_AUTHORING("CH", "ch-authoring"),
		IE_AUTHORING("IE", "ie-authoring"),
		EE_AUTHORING("EE", "ee-authoring"),
		NZ_AUTHORING("NZ", "nz-authoring"),
		ZH_AUTHORING("ZH", "zh-authoring"),
        KR_AUTHORING("KR", "kr-authoring"),
		FIRST_TIME_LOINC_VALIDATION ("LOINC", "first-time-loinc-validation"),
		FIRST_TIME_COMMON_EDITION_VALIDATION ("COMMON", "first-time-common-edition"),
		LOINC_EDITION ("LOINC", "LoincEdition"),
		DANISH_EDITION("DK", "DanishEdition"),
		SWEDISH_EDITION("SE", "SwedishEdition"),
		US_EDITION("US", "USEdition"),
		BE_EDITION("BE", "BelgianEdition"),
		NO_EDITION("NO", "NorwegianEdition"),
		CH_EDITION("CH", "SwissEdition"),
		IE_EDITION("IE", "IrishEdition"),
		EE_EDITION("EE", "EstonianEdition"),
		GPFP_ICPC2("GPFP-ICPC2","GPFP-ICPC2"),
		GMDN("GMDN","GMDN"),
		STATED_RELATIONSHIPS_VALIDATION("STATED_RELATIONSHIPS","stated-relationships-validation"), //Assertions group that contains only stated relationship for file centric and component centric assertions
		STATED_RELATIONSHIPS_RELEASE_VALIDATION("STATED_RELATIONSHIPS_RELEASE_TYPE","stated-relationships-release-validation"), //Assertion group that contains full list of stated relationship assertions
		DERIVATIVE_EDITION("DERIVATIVE","DerivativeEdition");


		private String name;
		private String releaseCenter;
		AssertionGroupName(String releaseCenter, String name) {
			this.releaseCenter = releaseCenter;
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
		public String getReleaseCenter() {
			return this.releaseCenter;
		}

	}

	public enum ProductName {
		// TODO FRI-246
		// add the new edition to FILENAME_PATTERN_TO_EDITION_MAP as well in ReleaseDataManager.
			INT("INT", "900000000000207008"),
			AU("AU", "32506021000036107"),
			BE("BE", "11000172109"),
			NL("NL", "11000146104"),
			UK("UK", "999000041000000102"),
			UKCL("UKCL", "999000011000000103"),
			US("US", "731000124108"),
			NZ("NZ", "21000210109"),
			ES("ES", "450829007"),

			DK("DK", "554471000005108"),
			SE("SE", "45991000052106"),
			NO("NO", "51000202101"),
			CH("CH", "2011000195101"),
			IE("IE", "11000220105"),
			EE("EE", "11000181102"),
			AT("AT", "11000234105"),
			TM("TM", "895344001"),
			SV("SNOVET", "332351000009108");

			private String name;
			private String moduleId;
			ProductName(String name, String moduleId) {
					this.name = name;
					this.moduleId = moduleId;
			}

			public String getName() {
					return this.name;
			}
			public String getModuleId() {
					return this.moduleId;
			}
			static public String toModuleId(String name) {
				for (ProductName pn: ProductName.values()) {
					if (name.equalsIgnoreCase(pn.getName())) {
						return pn.getModuleId();
					}
				}
				return name;
			}
	}

	private static final String SIMPLE_MAP = "simple map";
	private static final String RESOURCE = "resource";
	private static final String LOINC = "LOINC";
	private static final String ICD_9_COMPLEX_MAP ="ICD-9-CM";
	@Autowired
	private AssertionService assertionService;

	private static final String[] SPANISH_EXTENSION_EXCLUDE_LIST = {"dd0d0406-7481-444a-9f04-b6fc7db49039","c3249e80-84f0-11e1-b0c4-0800200c9a66"};

	private static final String[] SNAPSHOT_EXCLUDE_LIST = {"4dbfed80-79b9-11e1-b0c4-0800200c9a66",
		"5c6b6bc0-79b9-11e1-b0c4-0800200c9a66",
		"88315a11-4e71-49d2-977f-a5d5ac2a4dc4",
		"2e4fd620-7d08-11e1-b0c4-0800200c9a66",
		"6dbaed71-f031-4290-b74f-f35561c2e283",
		"c2975dd5-3869-4bf7-ac75-53fd53b90144"};

	private static final String[] COMMON_AUTHORING_ONLY_LIST = {"a49fabee-0d72-41b0-957d-32983c79f26c"};

	//SNOMED RT Identifier is deprecated from the international 20170731 release onwards.
	private static final String[] SNOMED_RT_IDENTIFIER_ASSERTIONS = {"730720b0-7f25-11e1-b0c4-0800200c9a66","83638340-7f25-11e1-b0c4-0800200c9a66",
																	"5e80ea3e-c4dd-4ae3-8b75-f0567e42b962","695cea40-7f25-11e1-b0c4-0800200c9a66"};

	private static final String[] US_AUTHORING_EXCLUDE_LIST = {"31f5e2c8-b0b9-42ee-a9bf-87d95edad83b","6bed3e87-6d20-4b05-81ce-43d359a6f684"};

	private static final String[] US_EDITION_EXCLUDE_LIST = {"31f5e2c8-b0b9-42ee-a9bf-87d95edad83b","2e2542f9-64a4-43d0-bf4e-9029af8b7cf0"};

	private static final String[] INT_AUTHORING_EXCLUDE_LIST = {"6bed3e87-6d20-4b05-81ce-43d359a6f684"};

    private static final String[] INT_AUTHORING_INCLUDE_LIST = {
            "11735994-4b7f-4e61-9bde-059b7e085031",
            "26c25479-c3ba-47f2-9851-bb05ae42ad48",
            "2aa0bea1-b1fa-4543-b277-b4392a6f864d",
            "2b193a88-8dab-4d19-b995-b556ed59398d",
            "35680574-3ac6-4b68-9efe-de88b677eb35",
            "372e59d5-d6f3-4708-93d0-6cb92da69006",
            "4478a896-2724-4417-8bce-8986ecc53c4e",
            "44916964-5b78-4842-81d8-e8293ee93bea",
            "4deb9274-d923-4f84-b0dc-c4dab0c2fc4c",
            "560e6d0c-64e8-4726-9516-ae7a7606b0b3",
            "5c6b6bc0-79b9-11e1-b0c4-0800200c9a66",
            "5f1a51a3-6200-4463-8799-d75998165278",
            "8864db45-0f97-4eda-b7ba-414289dd3a99",
            "910844a8-97e5-4096-add2-e1734b941e10",
            "9190473a-29f7-40fc-b879-9ae0d038b681",
            "9912342f-5010-40fd-9bea-301b737973a1",
            "a0a0b444-b1c7-4d31-ac45-c44f2e35c5a5",
            "b1ebc4cb-6a86-498f-a3ea-aeef250b9399",
            "b205d819-2ef6-48e3-8fe1-1793e83a7b8a",
            "b88b9f46-4c33-4d8e-b9ab-ddb87aef3068",
            "bc055a18-f93e-42fd-8bb2-347f2b2b8976",
            "ccd8f56e-45ec-40b7-9404-9b264881e64d",
            "cec2a50e-c04b-4442-a979-e8ad8f83ff91",
            "d182e8d4-c8e1-4e5f-ae17-b2f786f55727",
            "d7e12c75-9979-4adb-9b5b-3b8a80ab4fbb",
            "e6082dc4-c6f4-48c6-afa3-233182336a5c",
            "eff30fb0-7856-11e1-b0c4-0800200c9a66",
            "fbfc4fd1-f10d-4fc2-889f-df0e089df4b7",
            "202ef495-5f18-4e3e-8129-63759f2bbbd6"
    };

	private static final String[] MS_AUTHORING_INCLUDE_LIST = {"35680574-3ac6-4b68-9efe-de88b677eb35",
			"11735994-4b7f-4e61-9bde-059b7e085031",
			"4478a896-2724-4417-8bce-8986ecc53c4e",
			"372e59d5-d6f3-4708-93d0-6cb92da69006",
			"0cc708af-6816-4370-91be-dba8da99d227",
			"44916964-5b78-4842-81d8-e8293ee93bea",
			"fbfc4fd1-f10d-4fc2-889f-df0e089df4b7",
			"a0a0b444-b1c7-4d31-ac45-c44f2e35c5a5",
			"9912342f-5010-40fd-9bea-301b737973a1",
			"bc055a18-f93e-42fd-8bb2-347f2b2b8976",
			"cab60a2f-4239-4933-91d6-dc910a8ac08b",
			"5f1a51a3-6200-4463-8799-d75998165278",
			"2b193a88-8dab-4d19-b995-b556ed59398d",
			"e6082dc4-c6f4-48c6-afa3-233182336a5c",
			"b88b9f46-4c33-4d8e-b9ab-ddb87aef3068",
			"2aa0bea1-b1fa-4543-b277-b4392a6f864d",
			"88315a11-4e71-49d2-977f-a5d5ac2a4dc4",
			"9190473a-29f7-40fc-b879-9ae0d038b681",
			"eff30fb0-7856-11e1-b0c4-0800200c9a66",
			"89ceaf00-79b9-11e1-b0c4-0800200c9a66",
			"4deb9274-d923-4f84-b0dc-c4dab0c2fc4c",
			"560e6d0c-64e8-4726-9516-ae7a7606b0b3",
			"910844a8-97e5-4096-add2-e1734b941e10",
			"26c25479-c3ba-47f2-9851-bb05ae42ad48"
	};

	private static final String[] INT_EDITION_EXCLUDE_LIST = {"2e2542f9-64a4-43d0-bf4e-9029af8b7cf0"};

	private static final String[] FIRST_TIME_COMMON_ADDITIONAL_EXCLUDE_LIST = {"3cb10511-33b7-4eca-ba0e-93bcccf70d86", "48118153-d32a-4d1c-bfbc-23ed953e9991"};

	private static final String[] STATED_RELATIONSHIP_ASSERTIONS = {
			"994b5ff0-79b9-11e1-b0c4-0800200c9a66",
			"5555c400-7d08-11e1-b0c4-0800200c9a66",
			"4d32c9d0-7d08-11e1-b0c4-0800200c9a66",
			"cab60a2f-4239-4933-91d6-dc910a8ac08b",
			"a4fcd810-79b9-11e1-b0c4-0800200c9a66",
			"1b21ec4b-b6db-42fe-ae7e-f79e24e25b7f",
			"b1d75e1c-ea12-4567-a8b9-f69923a57cdf",
			"6dbaed71-f031-4290-b74f-f35561c2e283",
			"50e809a0-7d08-11e1-b0c4-0800200c9a66",
			"49487040-7d08-11e1-b0c4-0800200c9a66",
			"89ceaf00-79b9-11e1-b0c4-0800200c9a66",
			"23f18cc4-d2d7-4759-96e3-c7d0f0b30a3a",
			"01ce3ac9-d168-4333-99d7-8d2f228f5ec9",
			"7e453140-79b9-11e1-b0c4-0800200c9a66",
			"fd8ed390-94e9-4fd9-9e20-902a24273dca",
			"9f84d9a0-79b9-11e1-b0c4-0800200c9a66",
			"9074a620-79b9-11e1-b0c4-0800200c9a66",
			"5d27df10-7d08-11e1-b0c4-0800200c9a66",
			"2a938c7e-0803-44a1-8358-339daa87ee39",
			"f2293d20-7cd6-11e1-b0c4-0800200c9a66"
	};

	private static final String[] COMMON_LANGUAGE_REFSETS_ASSERTIONS = {
			"bd27d80a-42f1-4412-8111-fdf72fa32904",
			"73e46e72-6fdb-407b-9d50-d86a36a3a862",
			"d76f1430-7e9a-11e1-b0c4-0800200c9a66",
			"28c3a31d-7a9d-48f6-8eb9-18c25b6306fb",
			"f0b4c712-781a-4ca5-872e-883cf1949f12",
			"ffe6c560-7856-11e1-b0c4-0800200c9a66",
			"03cf9850-7857-11e1-b0c4-0800200c9a66",
			"c3249e80-84f0-11e1-b0c4-0800200c9a66",
			"31f5e2c8-b0b9-42ee-a9bf-87d95edad83b",
			"f636b7b0-7e9a-11e1-b0c4-0800200c9a66",
			"b87c6dfe-c109-4c8d-91f2-a15a17631ad2",
			"eff30fb0-7856-11e1-b0c4-0800200c9a66",
			"de0c4d6c-6297-41be-9979-fb1c93717baa",
			"f5d9ae70-7856-11e1-b0c4-0800200c9a66",
			"e658fb00-7e9a-11e1-b0c4-0800200c9a66",
			"91e26b70-ea15-4057-8c31-a1bf91614654",
			"a0a0b444-b1c7-4d31-ac45-c44f2e35c5a5",
			"fc24db60-7856-11e1-b0c4-0800200c9a66",
	};

	private static final String[] MRCM_REFSETS_ASSERTIONS = {
			"e968c5d1-e06b-4351-9e96-3f62208d27d4",
			"413c91cd-4b1a-47cf-a278-3141bea39fde",
			"292b4427-0cce-493d-b63c-27cd249eafea",
			"0bd6a234-58f9-424f-84b4-0dc800e5a70b",
			"0e8e3549-e704-41a2-a853-fd91a60116ee",
			"338c39ad-6f40-46dc-ad35-b7c697d66a63",
			"32bc5dc7-192c-441f-9693-6710020a6442",
			"14e30e46-1e7a-4655-b1ca-48254f8b7071",
			"c3e6008a-2e9a-4b42-9a37-78ea6a4c7799",
			"bd129ddf-e7ea-49a4-9e9c-ad790b75331d",
			"40b0b1f3-7ed0-4074-ab0e-c5abd19f70b8",
			"907c11ad-5ad7-49ed-b3e8-1149b1e95716",
			"618115ab-b5bc-4ed3-aaba-fec8536fe498",
			"b110b8de-53df-4387-b0ac-aecfbc01ed0d",
			"15a2289d-a160-4faa-b1ea-2f54980b5830",
			"844f4f15-ac5d-48a6-ba33-f11893cb7a20",
			"c4920cb7-7e7f-4f3f-ae87-f15b7c8fb1b6",
			"4fe314dd-6f65-4e9e-8ce1-a4c7db598c0d",
			"b1ebc4cb-6a86-498f-a3ea-aeef250b9399",
			"cec2a50e-c04b-4442-a979-e8ad8f83ff91",
			"8864db45-0f97-4eda-b7ba-414289dd3a99",
			"d7e12c75-9979-4adb-9b5b-3b8a80ab4fbb",
			"87af4df0-c506-41ed-9089-6a359f16b34f",
			"4fdf8c6a-8b58-4bf7-a8b5-71176ede7ac0",
			"becfbeaa-5eab-4a4a-90ca-7c893b7495b2",
			"0f7b6979-7a9d-45d4-956b-972d452f1638",
			"75afe90a-eb1c-4e86-9312-22bc742f5b49",
			"609ed083-737c-401a-9aa9-5666bbd2aff2",
			"56c6abd0-11c7-409e-b0ed-f9d0f08f2cd7",
			"0999e34c-36b5-4d33-951a-8e949a66062e",
			"501dc4c4-49d4-41d9-9751-271687d54820",
			"7c87a865-beb5-4e03-a489-2b05a40bd860",
			"504d10af-deba-46f0-85d5-f792749b0d62",
			"388b2eff-f8b0-418c-9794-d35aaa4d748b",
			"7c3fe37c-6ad5-446e-bc82-7c2bc078042a",
			"ad0f8978-c8a7-4d91-8e3e-a887e4830fec",
			"43bdeea9-be7b-4235-b58e-8842a8e47793",
			"054bccb0-c678-4722-94dd-b664e0c6267e",
			"b205d819-2ef6-48e3-8fe1-1793e83a7b8a",
			"d182e8d4-c8e1-4e5f-ae17-b2f786f55727",
			"23a71f2e-5c90-4090-af07-bcb3fa3d5673",
			"ccd8f56e-45ec-40b7-9404-9b264881e64d",
			"5d0fdbf5-fef6-44b2-aa42-ebb302132178",
			"9ece50b0-cafc-4e13-a66a-a59d315e9f2c",
			"a3e31a6c-0443-46a4-8b18-daad481247ed",
			"e980e959-943b-4cdc-998b-f91e670a030b",
			"a48d78fe-8ab5-4705-bbc5-1286fb2a98a2",
			"d51966ed-e034-4a25-90ef-1eb489c0bd37",
			"6f4aa58c-cc59-4a51-a630-fe4c69bac53b",
			"bd3bb9ec-97ae-4c53-9686-9c8c63b8d67b",
			"f1fc2c4b-9202-4a0b-9c00-49b6de7a7cc5",
			"ce8f4bae-0c51-4248-9c42-8f05a2cccdb4",
			"b36b78d8-13c3-4fc7-8632-0a03f725da17",
			"f47d1537-41dc-4cb5-ad0a-ed4caf993801",
			"73772817-4654-4abf-b801-e849fbae1ba0",
			"57ae69cd-26f0-4001-ba2f-ae56129e2e28"
	};

/* the following were included but feel that they should be validated for project level as well.
	"6b34ab30-79b9-11e1-b0c4-0800200c9a66",
	"72184790-79b9-11e1-b0c4-0800200c9a66",
	"77fc7550-79b9-11e1-b0c4-0800200c9a66",
	"32b41aa0-7d08-11e1-b0c4-0800200c9a66",
*/


	private static final Logger LOGGER = LoggerFactory.getLogger(AssertionGroupImporter.class);

	public boolean isImportRequired() {
		List<AssertionGroup> allGroups = assertionService.getAllAssertionGroups();
		if (allGroups == null || allGroups.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * Create assertion groups
	 *
	 * file-centric-validation
	 * release-type-validation
	 * component-centric-validation
	 * SpanishEdition
	 * InternationalEdition
	 * SnapshotContentValidation
	 */

	public void importAssertionGroups() {
		List<AssertionGroup> allGroups = assertionService.getAllAssertionGroups();
		List<String> existingGroups = new ArrayList<>();
		if (allGroups != null) {
			for (AssertionGroup group : allGroups) {
				LOGGER.info("Validation group is already created:" + group.getName());
				existingGroups.add(group.getName());
			}
		}
		List<Assertion> allAssertions = assertionService.findAll();
		for ( AssertionGroupName groupName : AssertionGroupName.values()) {
			if (!existingGroups.contains(groupName.getName())) {
				LOGGER.info("creating assertion group:" + groupName.getName());
				createAssertionGroup(groupName, allAssertions);
				LOGGER.info("assertion group created:" + groupName.getName());
			}
		}
	}


	private void createAssertionGroup(AssertionGroupName groupName, List<Assertion> allAssertions) {

		switch (groupName) {
			case FILE_CENTRIC_VALIDATION :
			case RELEASE_TYPE_VALIDATION :
			case COMPONENT_CENTRIC_VALIDATION :
				createAssertionGroupByKeyWord(allAssertions, groupName.getName());
				break;
			case MDRS_VALIDATION :
				createAssertionGroup(getReleaseAssertionsByCenter(allAssertions, groupName.getName()), groupName.getName());
				break;
			case LOINC_EDITION :
			case SPANISH_EDITION :
			case DANISH_EDITION	:
			case SWEDISH_EDITION :
			case INTERNATIONAL_EDITION :
			case US_EDITION :
			case BE_EDITION :
			case COMMON_EDITION :
			case NO_EDITION :
			case CH_EDITION :
			case IE_EDITION :
			case GMDN :
			case EE_EDITION :
			case GPFP_ICPC2 :
			case DERIVATIVE_EDITION :
				createReleaseAssertionGroup(allAssertions, groupName);
				break;
			case COMMON_AUTHORING :
				createCommonSnapshotAssertionGroup(allAssertions);
				break;
			case COMMON_AUTHORING_WITHOUT_LANG_REFSETS :
				createCommonSnapshotWithoutLangRefsetsAssertionGroup(allAssertions);
				break;
			case INT_AUTHORING :
			case AT_AUTHORING:
			case DK_AUTHORING :
			case SE_AUTHORING :
			case US_AUTHORING :
			case BE_AUTHORING :
			case NO_AUTHORING :
			case CH_AUTHORING :
			case IE_AUTHORING :
			case NZ_AUTHORING:
			case ZH_AUTHORING:
			case EE_AUTHORING :
            case KR_AUTHORING:
				createSnapshotAssertionGroup(groupName);
				break;
			case FIRST_TIME_LOINC_VALIDATION :
			case FIRST_TIME_COMMON_EDITION_VALIDATION :
				createFirstTimeReleaseGroup(allAssertions, groupName);
				break;
			case STATED_RELATIONSHIPS_VALIDATION:
				createStatedRelationshipGroup(allAssertions, groupName, false);
				break;
			case STATED_RELATIONSHIPS_RELEASE_VALIDATION:
				createStatedRelationshipGroup(allAssertions, groupName, true);
				break;
			default :
					 LOGGER.warn("unrecognized group: " + groupName.getName());
			  break;
		}

	}

	private void createFirstTimeReleaseGroup(List<Assertion> allAssertions, AssertionGroupName groupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(groupName.getName());
		group = assertionService.createAssertionGroup(group);
		int counter = 0;
		String keyWords;
		for (Assertion assertion : allAssertions) {
			if (Arrays.asList(COMMON_AUTHORING_ONLY_LIST).contains(assertion.getUuid().toString()) || Arrays.asList(FIRST_TIME_COMMON_ADDITIONAL_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			keyWords=assertion.getKeywords();
			//exclude SNOMED RT assertions
			if ( Arrays.asList(SNOMED_RT_IDENTIFIER_ASSERTIONS).contains(assertion.getUuid().toString())) {
				continue;
			}
			// Exclude stated relationship assertions
			if (Arrays.asList(STATED_RELATIONSHIP_ASSERTIONS).contains(assertion.getUuid().toString())) {
				continue;
			}

			if (RELEASE_TYPE_VALIDATION.getName().equals(keyWords) || FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)
					|| keyWords.contains("," + groupName.getReleaseCenter())) {
				String assertionText = assertion.getAssertionText();
				if ( !assertionText.contains(PREVIOUS) && !assertionText.contains(NEW_INACTIVE_STATES_FOLLOW_ACTIVE_STATES) ) {
					assertionService.addAssertionToGroup(assertion, group);
					counter++;
				}
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}

	private void createCommonSnapshotAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(AssertionGroupName.COMMON_AUTHORING.getName());
		group = assertionService.createAssertionGroup(group);
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			String keyWords = assertion.getKeywords();
			 if (keyWords.contains(RELEASE_TYPE_VALIDATION.getName())) {
				 continue;
			 }
			if (FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (Arrays.asList(SNAPSHOT_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
					continue;
				}
				//exclude simple map file checking as term server extracts don't contain these
				if (assertion.getAssertionText().contains(SIMPLE_MAP)) {
					continue;
				}
				// Exclude stated relationship assertions
				if (Arrays.asList(STATED_RELATIONSHIP_ASSERTIONS).contains(assertion.getUuid().toString())) {
					continue;
				}
				assertionService.addAssertionToGroup(assertion, group);
				counter++;
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}

	private void createCommonSnapshotWithoutLangRefsetsAssertionGroup(List<Assertion> allAssertions) {
		AssertionGroup group = new AssertionGroup();
		group.setName(AssertionGroupName.COMMON_AUTHORING_WITHOUT_LANG_REFSETS.getName());
		group = assertionService.createAssertionGroup(group);
		int counter = 0;
		for (Assertion assertion : allAssertions) {
			String keyWords = assertion.getKeywords();
			if (keyWords.contains(RELEASE_TYPE_VALIDATION.getName())) {
				continue;
			}
			if (FILE_CENTRIC_VALIDATION.getName().equals(keyWords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keyWords)) {
				//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
				if (Arrays.asList(SNAPSHOT_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
					continue;
				}
				//exclude simple map file checking as term server extracts don't contain these
				if (assertion.getAssertionText().contains(SIMPLE_MAP)) {
					continue;
				}
				// Exclude stated relationship assertions
				if (Arrays.asList(STATED_RELATIONSHIP_ASSERTIONS).contains(assertion.getUuid().toString())) {
					continue;
				}

				if (Arrays.asList(COMMON_LANGUAGE_REFSETS_ASSERTIONS).contains(assertion.getUuid().toString())) {
					continue;
				}
				assertionService.addAssertionToGroup(assertion, group);
				counter++;
			}
		}
		LOGGER.info("Total assertions added {} for assertion group {}", counter, group.getName() );
	}


	private void createSnapshotAssertionGroup(AssertionGroupName groupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(groupName.getName());
		group = assertionService.createAssertionGroup(group);
		List<Assertion> allAssertions = assertionService.getAssertionsByKeyWords("," + groupName.getReleaseCenter(), false);
		List<Assertion> releaseTypeAssertions = assertionService.getAssertionsByKeyWords(RELEASE_TYPE_VALIDATION.getName(), false);
		if (INT_AUTHORING.equals(groupName)) {
            allAssertions.addAll(releaseTypeAssertions.stream().filter(assertion -> Arrays.asList(INT_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString())).collect(Collectors.toList()));
		} else {
			if (!US_AUTHORING.equals(groupName)) {
				allAssertions.addAll(assertionService.getAssertionsByKeyWords(",EXTENSION", false));
			}
			allAssertions.addAll(releaseTypeAssertions.stream().filter(assertion -> Arrays.asList(MS_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString())).collect(Collectors.toList()));
        }
		for (Assertion assertion : allAssertions) {
			if (assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION.getName()) && ((!AssertionGroupName.INT_AUTHORING.equals(groupName) && !Arrays.asList(MS_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString()))
																					|| (AssertionGroupName.INT_AUTHORING.equals(groupName) && !Arrays.asList(INT_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString())))) {
				continue;
			}
			//exclude this from snapshot group as termserver extracts for inferred relationship file doesn't reuse existing ids.
			if (Arrays.asList(SNAPSHOT_EXCLUDE_LIST).contains(assertion.getUuid().toString()) && ((!AssertionGroupName.INT_AUTHORING.equals(groupName) && !Arrays.asList(MS_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString()))
																								|| (AssertionGroupName.INT_AUTHORING.equals(groupName) && !Arrays.asList(INT_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString())))) {
				continue;
			}
			// Exclude stated relationship assertions
			if (Arrays.asList(STATED_RELATIONSHIP_ASSERTIONS).contains(assertion.getUuid().toString()) && !Arrays.asList(MS_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			//exclude simple map file checking as term server extracts don't contain these
			if (assertion.getAssertionText().contains(SIMPLE_MAP) && ((!AssertionGroupName.INT_AUTHORING.equals(groupName) && !Arrays.asList(MS_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString()))
																	|| (AssertionGroupName.INT_AUTHORING.equals(groupName) && !Arrays.asList(INT_AUTHORING_INCLUDE_LIST).contains(assertion.getUuid().toString())))) {
				continue;
			}
			if (AssertionGroupName.US_AUTHORING.equals(groupName) && Arrays.asList(US_AUTHORING_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			if (AssertionGroupName.INT_AUTHORING.equals(groupName) && Arrays.asList(INT_AUTHORING_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			assertionService.addAssertionToGroup(assertion, group);
		}
		LOGGER.info("Total assertions added {} for assertion group {}", allAssertions.size(), group.getName() );
	}


	private List<Assertion> getCommonReleaseAssertions(List<Assertion> allAssertions) {
		List<Assertion> result = new ArrayList<>();
		String keywords;
		for (Assertion assertion : allAssertions) {
			 keywords = assertion.getKeywords();
			if (keywords.contains(MDRS) || FILE_CENTRIC_VALIDATION.getName().equals(keywords) || COMPONENT_CENTRIC_VALIDATION.getName().equals(keywords)
					|| RELEASE_TYPE_VALIDATION.getName().equals(keywords)) {
				result.add(assertion);
			}
		}
		LOGGER.info("Total common release assertions:" + result.size());
		return result;
	}

	private List<Assertion> getReleaseAssertionsByCenter(List<Assertion> allAssertions, String releaseCenter) {
		List<Assertion> result = new ArrayList<>();
		for (Assertion assertion : allAssertions) {
			String keywords = assertion.getKeywords();
			if (keywords.endsWith("," + releaseCenter) || keywords.contains("," + releaseCenter + ",")) {
				result.add(assertion);
			}
		}
		LOGGER.info("Total release assertions found :{} for center:{}", result.size(), releaseCenter);
		return result;
	}

	private void createReleaseAssertionGroup(List<Assertion> allAssertions, AssertionGroupName groupName) {
		//create international assertion group
		AssertionGroup assertionGroup = new AssertionGroup();
		assertionGroup.setName(groupName.getName());
		assertionGroup = assertionService.createAssertionGroup(assertionGroup);
		List<Assertion> assertionsToBeAdded = getCommonReleaseAssertions(allAssertions);
		assertionsToBeAdded.addAll(getReleaseAssertionsByCenter(allAssertions, groupName.getReleaseCenter()));
		if (!AssertionGroupName.COMMON_EDITION.equals(groupName) && !AssertionGroupName.INTERNATIONAL_EDITION.equals(groupName) && !AssertionGroupName.US_EDITION.equals(groupName)) {
			assertionsToBeAdded.addAll(getReleaseAssertionsByCenter(allAssertions, "EXTENSION"));
		}
		for (Assertion assertion : assertionsToBeAdded) {
			if (Arrays.asList(COMMON_AUTHORING_ONLY_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			if ( AssertionGroupName.SPANISH_EDITION.equals(groupName) && Arrays.asList(SPANISH_EXTENSION_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			// Exclude stated relationship assertions
			if (Arrays.asList(STATED_RELATIONSHIP_ASSERTIONS).contains(assertion.getUuid().toString())) {
				continue;
			}
			//exclude SNOMED RT assertions
			if ( AssertionGroupName.INTERNATIONAL_EDITION.equals(groupName) && (Arrays.asList(SNOMED_RT_IDENTIFIER_ASSERTIONS).contains(assertion.getUuid().toString())
																				|| Arrays.asList(INT_EDITION_EXCLUDE_LIST).contains(assertion.getUuid().toString()))) {
				continue;
			}
			if ( AssertionGroupName.US_EDITION.equals(groupName) && Arrays.asList(US_EDITION_EXCLUDE_LIST).contains(assertion.getUuid().toString())) {
				continue;
			}
			if ( AssertionGroupName.DERIVATIVE_EDITION.equals(groupName) && Arrays.asList(MRCM_REFSETS_ASSERTIONS).contains(assertion.getUuid().toString())) {
				continue;
			}
			assertionService.addAssertionToGroup(assertion, assertionGroup);
		}
		LOGGER.info("Total assertions added {} for assertion group {}", assertionsToBeAdded.size(), groupName.getName() );
	}

	private void createAssertionGroupByKeyWord(List<Assertion> allAssertions, String assertionGroupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(assertionGroupName);
		group = assertionService.createAssertionGroup(group);
		for (Assertion assertion : allAssertions) {
			if (assertion.getKeywords().equals(assertionGroupName)) {
				assertionService.addAssertionToGroup(assertion, group);
			}
		}
	}

	private void createAssertionGroup(List<Assertion> allAssertions, String assertionGroupName) {
		AssertionGroup group = new AssertionGroup();
		group.setName(assertionGroupName);
		group = assertionService.createAssertionGroup(group);
		int addedAssertions = 0;
		for (Assertion assertion : allAssertions) {
				assertionService.addAssertionToGroup(assertion, group);
				addedAssertions++;
		}
		LOGGER.info("Total assertions added {} for assertion group {}", addedAssertions, assertionGroupName );
	}

	private void createStatedRelationshipGroup(List<Assertion> allAssertions, AssertionGroupName assertionGroupName, boolean useFullList) {
		AssertionGroup group = new AssertionGroup();
		group.setName(assertionGroupName.getName());
		group = assertionService.createAssertionGroup(group);
		List<String> statedRelationshipAssertionIds = Arrays.asList(STATED_RELATIONSHIP_ASSERTIONS);
		int statedRelationshipAssertionCount = statedRelationshipAssertionIds.size();
		int count = 0;
		for (Assertion assertion : allAssertions) {
			if(statedRelationshipAssertionIds.contains(assertion.getUuid().toString())) {
				if(useFullList || !(assertion.getKeywords().contains(RELEASE_TYPE_VALIDATION.getName()))) {
					assertionService.addAssertionToGroup(assertion, group);
				}
				count++;
				if(count == statedRelationshipAssertionCount) break;
			}
		}

	}

}
