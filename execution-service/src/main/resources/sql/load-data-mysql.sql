/* loads the SNOMED CT 'Full', 'Snapshot' and 'Delta' release - replace filenames with relevant locations of base SNOMED CT release files*/
use rvf_int_<release_version>;

/* Filenames may need to change depending on the release you wish to upload, currently set to July 2014 release */

/* * * * *  FULL * * * * */
load data local 
	infile '<data_location>/<data_location>/RF2Release/Full/Terminology/sct2_Concept_Full_INT_<release_version>.txt' 
	into table concept_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Terminology/sct2_Description_Full-en_INT_<release_version>.txt' 
	into table description_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Terminology/sct2_TextDefinition_Full-en_INT_<release_version>.txt' 
	into table textdefinition_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Terminology/sct2_Relationship_Full_INT_<release_version>.txt' 
	into table relationship_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Terminology/sct2_StatedRelationship_Full_INT_<release_version>.txt' 
	into table stated_relationship_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Refset/Language/der2_cRefset_LanguageFull-en_INT_<release_version>.txt' 
	into table langrefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Refset/Content/der2_cRefset_AssociationReferenceFull_INT_<release_version>.txt' 
	into table associationrefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Refset/Content/der2_cRefset_AttributeValueFull_INT_<release_version>.txt' 
	into table attributevaluerefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Refset/Map/der2_sRefset_SimpleMapFull_INT_<release_version>.txt' 
	into table simplemaprefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Refset/Content/der2_Refset_SimpleFull_INT_<release_version>.txt' 
	into table simplerefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Full/Refset/Map/der2_iissscRefset_ComplexMapFull_INT_<release_version>.txt' 
	into table complexmaprefset_f
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;


/* * * * *  Snapshot * * * * */
load data local 
	infile '<data_location>/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_<release_version>.txt' 
	into table concept_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_<release_version>.txt' 
	into table description_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Terminology/sct2_TextDefinition_Snapshot-en_INT_<release_version>.txt' 
	into table textdefinition_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Terminology/sct2_Relationship_Snapshot_INT_<release_version>.txt' 
	into table relationship_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Terminology/sct2_StatedRelationship_Snapshot_INT_<release_version>.txt' 
	into table stated_relationship_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Refset/Language/der2_cRefset_LanguageSnapshot-en_INT_<release_version>.txt' 
	into table langrefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Refset/Content/der2_cRefset_AssociationReferenceSnapshot_INT_<release_version>.txt' 
	into table associationrefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Refset/Content/der2_cRefset_AttributeValueSnapshot_INT_<release_version>.txt' 
	into table attributevaluerefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Refset/Map/der2_sRefset_SimpleMapSnapshot_INT_<release_version>.txt' 
	into table simplemaprefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Refset/Content/der2_Refset_SimpleSnapshot_INT_<release_version>.txt' 
	into table simplerefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Snapshot/Refset/Map/der2_iissscRefset_ComplexMapSnapshot_INT_<release_version>.txt' 
	into table complexmaprefset_s
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

/* * * * *  Delta * * * * */
load data local 
	infile '<data_location>/RF2Release/Delta/Terminology/sct2_Concept_Delta_INT_<release_version>.txt' 
	into table concept_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Terminology/sct2_Description_Delta-en_INT_<release_version>.txt' 
	into table description_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Terminology/sct2_TextDefinition_Delta-en_INT_<release_version>.txt' 
	into table textdefinition_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Terminology/sct2_Relationship_Delta_INT_<release_version>.txt' 
	into table relationship_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Terminology/sct2_StatedRelationship_Delta_INT_<release_version>.txt' 
	into table stated_relationship_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Refset/Language/der2_cRefset_LanguageDelta-en_INT_<release_version>.txt' 
	into table langrefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Refset/Content/der2_cRefset_AssociationReferenceDelta_INT_<release_version>.txt' 
	into table associationrefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Refset/Content/der2_cRefset_AttributeValueDelta_INT_<release_version>.txt' 
	into table attributevaluerefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Refset/Map/der2_sRefset_SimpleMapDelta_INT_<release_version>.txt' 
	into table simplemaprefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Refset/Content/der2_Refset_SimpleDelta_INT_<release_version>.txt' 
	into table simplerefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/RF2Release/Delta/Refset/Map/der2_iissscRefset_ComplexMapDelta_INT_<release_version>.txt' 
	into table complexmaprefset_d
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;























