/* loads the SNOMED CT 'Full', 'Snapshot' and 'Delta' release - rvf replaces filenames with relevant locations of base SNOMED CT release files*/
use rvf_int_<release_version>;

/* Filenames will to change depending on the release you wish to upload */

/* * * * *  FULL * * * * */
load data local 
	infile '<data_location>/sct2_Concept_Full_INT_<release_version>.txt'
	into table concept_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_Description_Full-en_INT_<release_version>.txt' 
	into table description_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_TextDefinition_Full-en_INT_<release_version>.txt' 
	into table textdefinition_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_Relationship_Full_INT_<release_version>.txt' 
	into table relationship_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_StatedRelationship_Full_INT_<release_version>.txt' 
	into table stated_relationship_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_LanguageFull-en_INT_<release_version>.txt' 
	into table langrefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_AssociationReferenceFull_INT_<release_version>.txt' 
	into table associationrefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_AttributeValueFull_INT_<release_version>.txt' 
	into table attributevaluerefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_sRefset_SimpleMapFull_INT_<release_version>.txt' 
	into table simplemaprefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_Refset_SimpleFull_INT_<release_version>.txt' 
	into table simplerefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_iissscRefset_ComplexMapFull_INT_<release_version>.txt' 
	into table complexmaprefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local
	infile '<data_location>/der2_cRefset_MRCMModuleScopeFull_INT_<release_version>.txt'
	into table mrcmmodulescoperefset_f
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_ssccRefset_MRCMAttributeRangeFull_INT_<release_version>.txt'
	into table mrcmattributerangerefset_f
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_sssssssRefset_MRCMDomainFull_INT_<release_version>.txt'
	into table mrcmdomainrefset_f
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_cissccRefset_MRCMAttributeDomainFull_INT_<release_version>.txt'
	into table mrcmattributedomainrefset_f
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_ciRefset_DescriptionTypeFull_INT_<release_version>.txt'
	into table descriptiontyperefset_f
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

/* * * * *  Snapshot * * * * */
load data local 
	infile '<data_location>/sct2_Concept_Snapshot_INT_<release_version>.txt' 
	into table concept_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_Description_Snapshot-en_INT_<release_version>.txt' 
	into table description_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_TextDefinition_Snapshot-en_INT_<release_version>.txt' 
	into table textdefinition_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_Relationship_Snapshot_INT_<release_version>.txt' 
	into table relationship_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_StatedRelationship_Snapshot_INT_<release_version>.txt' 
	into table stated_relationship_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_LanguageSnapshot-en_INT_<release_version>.txt' 
	into table langrefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_AssociationReferenceSnapshot_INT_<release_version>.txt' 
	into table associationrefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_AttributeValueSnapshot_INT_<release_version>.txt' 
	into table attributevaluerefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_sRefset_SimpleMapSnapshot_INT_<release_version>.txt' 
	into table simplemaprefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_Refset_SimpleSnapshot_INT_<release_version>.txt' 
	into table simplerefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_iissscRefset_ComplexMapSnapshot_INT_<release_version>.txt' 
	into table complexmaprefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local
	infile '<data_location>/der2_cRefset_MRCMModuleScopeSnapshot_INT_<release_version>.txt'
	into table mrcmmodulescoperefset_s
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_ssccRefset_MRCMAttributeRangeSnapshot_INT_<release_version>.txt'
	into table mrcmattributerangerefset_s
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_sssssssRefset_MRCMDomainSnapshot_INT_<release_version>.txt'
	into table mrcmdomainrefset_s
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_cissccRefset_MRCMAttributeDomainSnapshot_INT_<release_version>.txt'
	into table mrcmattributedomainrefset_s
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_ciRefset_DescriptionTypeSnapshot_INT_<release_version>.txt'
	into table descriptiontyperefset_s
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

/* * * * *  Delta * * * * */
load data local 
	infile '<data_location>/sct2_Concept_Delta_INT_<release_version>.txt' 
	into table concept_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_Description_Delta-en_INT_<release_version>.txt' 
	into table description_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_TextDefinition_Delta-en_INT_<release_version>.txt' 
	into table textdefinition_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_Relationship_Delta_INT_<release_version>.txt' 
	into table relationship_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/sct2_StatedRelationship_Delta_INT_<release_version>.txt' 
	into table stated_relationship_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_LanguageDelta-en_INT_<release_version>.txt' 
	into table langrefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_AssociationReferenceDelta_INT_<release_version>.txt' 
	into table associationrefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_cRefset_AttributeValueDelta_INT_<release_version>.txt' 
	into table attributevaluerefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_sRefset_SimpleMapDelta_INT_<release_version>.txt' 
	into table simplemaprefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_Refset_SimpleDelta_INT_<release_version>.txt' 
	into table simplerefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/der2_iissscRefset_ComplexMapDelta_INT_<release_version>.txt' 
	into table complexmaprefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local
	infile '<data_location>/der2_cRefset_MRCMModuleScopeDelta_INT_<release_version>.txt'
	into table mrcmmodulescoperefset_d
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_ssccRefset_MRCMAttributeRangeDelta_INT_<release_version>.txt'
	into table mrcmattributerangerefset_d
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_sssssssRefset_MRCMDomainDelta_INT_<release_version>.txt'
	into table mrcmdomainrefset_d
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_cissccRefset_MRCMAttributeDomainDelta_INT_<release_version>.txt'
	into table mrcmattributedomainrefset_d
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;

load data local
	infile '<data_location>/der2_ciRefset_DescriptionTypeDelta_INT_<release_version>.txt'
	into table descriptiontyperefset_d
	columns terminated by '\t'
	lines terminated by '\r\n'
	ignore 1 lines;
