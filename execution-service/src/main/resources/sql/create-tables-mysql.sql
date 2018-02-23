/* create the Full SCT data tables */

drop table if exists concept_f;
create table concept_f(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
definitionstatusid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_definitionstatusid(definitionstatusid)
) engine=myisam default charset=utf8;


drop table if exists description_f;
create table description_f(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
conceptid bigint(20) not null,
languagecode varchar(2) not null,
typeid bigint(20) not null,
term varchar(256) not null collate utf8_bin,
casesignificanceid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists textdefinition_f;
create table textdefinition_f(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
conceptid bigint(20) not null,
languagecode varchar(2) not null,
typeid bigint(20) not null,
term varchar(1024) not null,
casesignificanceid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists relationship_f;
create table relationship_f(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
destinationid bigint(20) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
key idx_destinationid(destinationid),
key idx_relationshipgroup(relationshipgroup),
key idx_typeid(typeid),
key idx_characteristictypeid(characteristictypeid),
key idx_modifierid(modifierid)
) engine=myisam default charset=utf8;

drop table if exists stated_relationship_f;
create table stated_relationship_f(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
destinationid bigint(20) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
key idx_destinationid(destinationid),
key idx_relationshipgroup(relationshipgroup),
key idx_typeid(typeid),
key idx_characteristictypeid(characteristictypeid),
key idx_modifierid(modifierid)
) engine=myisam default charset=utf8;

drop table if exists langrefset_f;
create table langrefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
acceptabilityid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_acceptabilityid(acceptabilityid)
) engine=myisam default charset=utf8;

drop table if exists associationrefset_f;
create table associationrefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
targetcomponentid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_targetcomponentid(targetcomponentid)
) engine=myisam default charset=utf8;

drop table if exists attributevaluerefset_f;
create table attributevaluerefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
valueid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_valueid(valueid)
) engine=myisam default charset=utf8;

drop table if exists simplemaprefset_f;
create table simplemaprefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
maptarget varchar(32) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_maptarget(maptarget)
) engine=myisam default charset=utf8;

drop table if exists simplerefset_f;
create table simplerefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists complexmaprefset_f;
create table complexmaprefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule varchar(300),
	mapAdvice varchar(300),
	mapTarget varchar(10),
	correlationId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;

/* create the Snapshot S-CT data tables */

drop table if exists concept_s;
create table concept_s(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
definitionstatusid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_definitionstatusid(definitionstatusid)
) engine=myisam default charset=utf8;


drop table if exists description_s;
create table description_s(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
conceptid bigint(20) not null,
languagecode varchar(2) not null,
typeid bigint(20) not null,
term varchar(256) not null collate utf8_bin,
casesignificanceid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid),
key idx_term(term)
) engine=myisam default charset=utf8;

drop table if exists textdefinition_s;
create table textdefinition_s(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
conceptid bigint(20) not null,
languagecode varchar(2) not null,
typeid bigint(20) not null,
term varchar(1024) not null,
casesignificanceid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists relationship_s;
create table relationship_s(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
destinationid bigint(20) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
key idx_destinationid(destinationid),
key idx_relationshipgroup(relationshipgroup),
key idx_typeid(typeid),
key idx_characteristictypeid(characteristictypeid),
key idx_modifierid(modifierid)
) engine=myisam default charset=utf8;

drop table if exists stated_relationship_s;
create table stated_relationship_s(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
destinationid bigint(20) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
key idx_destinationid(destinationid),
key idx_relationshipgroup(relationshipgroup),
key idx_typeid(typeid),
key idx_characteristictypeid(characteristictypeid),
key idx_modifierid(modifierid)
) engine=myisam default charset=utf8;

drop table if exists langrefset_s;
create table langrefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
acceptabilityid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_acceptabilityid(acceptabilityid)
) engine=myisam default charset=utf8;

drop table if exists associationrefset_s;
create table associationrefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
targetcomponentid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_targetcomponentid(targetcomponentid)
) engine=myisam default charset=utf8;

drop table if exists attributevaluerefset_s;
create table attributevaluerefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
valueid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_valueid(valueid)
) engine=myisam default charset=utf8;

drop table if exists simplemaprefset_s;
create table simplemaprefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
maptarget varchar(32) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_maptarget(maptarget)
) engine=myisam default charset=utf8;

drop table if exists simplerefset_s;
create table simplerefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists complexmaprefset_s;
create table complexmaprefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule varchar(300),
	mapAdvice varchar(300),
	mapTarget varchar(10),
	correlationId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;


/* create the Delta S-CT data tables */

drop table if exists concept_d;
create table concept_d(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
definitionstatusid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_definitionstatusid(definitionstatusid)
) engine=myisam default charset=utf8;


drop table if exists description_d;
create table description_d(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
conceptid bigint(20) not null,
languagecode varchar(2) not null,
typeid bigint(20) not null,
term varchar(256) not null collate utf8_bin,
casesignificanceid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_term(term),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists textdefinition_d;
create table textdefinition_d(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
conceptid bigint(20) not null,
languagecode varchar(2) not null,
typeid bigint(20) not null,
term varchar(1024) not null,
casesignificanceid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists relationship_d;
create table relationship_d(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
destinationid bigint(20) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
key idx_destinationid(destinationid),
key idx_relationshipgroup(relationshipgroup),
key idx_typeid(typeid),
key idx_characteristictypeid(characteristictypeid),
key idx_modifierid(modifierid)
) engine=myisam default charset=utf8;

drop table if exists stated_relationship_d;
create table stated_relationship_d(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
destinationid bigint(20) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
key idx_destinationid(destinationid),
key idx_relationshipgroup(relationshipgroup),
key idx_typeid(typeid),
key idx_characteristictypeid(characteristictypeid),
key idx_modifierid(modifierid)
) engine=myisam default charset=utf8;

drop table if exists langrefset_d;
create table langrefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
acceptabilityid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_acceptabilityid(acceptabilityid)
) engine=myisam default charset=utf8;

drop table if exists associationrefset_d;
create table associationrefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
targetcomponentid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_targetcomponentid(targetcomponentid)
) engine=myisam default charset=utf8;

drop table if exists attributevaluerefset_d;
create table attributevaluerefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
valueid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_valueid(valueid)
) engine=myisam default charset=utf8;

drop table if exists simplemaprefset_d;
create table simplemaprefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
refsetid bigint(20) not null,
referencedcomponentid bigint(20) not null,
maptarget varchar(32) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_maptarget(maptarget)
) engine=myisam default charset=utf8;

drop table if exists simplerefset_d;
create table simplerefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists complexmaprefset_d;
create table complexmaprefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule varchar(300),
	mapAdvice varchar(300),
	mapTarget varchar(10),
	correlationId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;

drop table if exists extendedmaprefset_f;
create table extendedmaprefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule varchar(300),
	mapAdvice varchar(300),
	mapTarget varchar(10),
	correlationId bigint(20) not null,
	mapCategoryId bigint(20),
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;


drop table if exists extendedmaprefset_d;
create table extendedmaprefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule varchar(300),
	mapAdvice varchar(300),
	mapTarget varchar(10),
	correlationId bigint(20) not null,
	mapCategoryId bigint(20),
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;

drop table if exists extendedmaprefset_s;
create table extendedmaprefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule varchar(300),
	mapAdvice varchar(300),
	mapTarget varchar(10),
	correlationId bigint(20) not null,
	mapCategoryId bigint(20),
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;


drop table if exists expressionAssociationRefset_s;
create table expressionAssociationRefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapTarget varchar(20) not null,
	expression varchar(500) not null,
	definitionStatusId bigint(20) not null,
	correlationId bigint(20) not null,
	contentOriginId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;


drop table if exists expressionAssociationRefset_d;
create table expressionAssociationRefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapTarget varchar(20) not null,
	expression varchar(500) not null,
	definitionStatusId bigint(20) not null,
	correlationId bigint(20) not null,
	contentOriginId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;


drop table if exists expressionAssociationRefset_f;
create table expressionAssociationRefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapTarget varchar(20) not null,
	expression varchar(500) not null,
	definitionStatusId bigint(20) not null,
	correlationId bigint(20) not null,
	contentOriginId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;



drop table if exists mapCorrelationOriginRefset_f;
create table mapCorrelationOriginRefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapTarget varchar(20) not null,
	attributeId bigint(20) not null,
	correlationId bigint(20) not null,
	contentOriginId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;

drop table if exists mapCorrelationOriginRefset_s;
create table mapCorrelationOriginRefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapTarget varchar(20) not null,
	attributeId bigint(20) not null,
	correlationId bigint(20) not null,
	contentOriginId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;

drop table if exists mapCorrelationOriginRefset_d;
create table mapCorrelationOriginRefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mapTarget varchar(20) not null,
	attributeId bigint(20) not null,
	correlationId bigint(20) not null,
	contentOriginId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;

drop table if exists mrcmAttributeDomainRefset_s;
create table mrcmAttributeDomainRefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainid bigint(20) not null,
	grouped char(1) not null,
	attributecardinality char(4) not null,
	attributeingroupcardinality char(4) not null,
	rulestrengthid bigint(20) not null,
	contenttypeid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_domainid(domainid),
	key idx_grouped(grouped),
	key idx_attributecardinality(attributecardinality),
	key idx_attributeingroupcardinality(attributeingroupcardinality),
	key idx_rulestrengthid(rulestrengthid),
	key idx_contenttypeid(contenttypeid)
) engine=myisam default charset=utf8;

drop table if exists mrcmModuleScopeRefset_s;
create table mrcmModuleScopeRefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mrcmrulerefsetid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mrcmrulerefsetid(mrcmrulerefsetid)
) engine=myisam default charset=utf8;

drop table if exists mrcmAttributeRangeRefset_s;
create table mrcmAttributeRangeRefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	rangeconstraint varchar(4000) not null,
	attributerule varchar(4000) not null,
	rulestrengthid bigint(20) not null,
	contenttypeid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_rulestrengthid(rulestrengthid),
	key idx_contenttypeid(contenttypeid)
) engine=myisam default charset=utf8;

drop table if exists mrcmDomainRefset_s;
create table mrcmDomainRefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainconstraint varchar(1024) not null,
	parentdomain varchar(1024) not null,
	proximalprimitiveconstraint varchar(4000) not null,
	proximalprimitiverefinement varchar(4000) not null,
	domaintemplateforprecoordination varchar(4000) not null,
	domaintemplateforpostcoordination varchar(4000) not null,
	guideurl varchar(255) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmAttributeDomainRefset_f;
create table mrcmAttributeDomainRefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainid bigint(20) not null,
	grouped char(1) not null,
	attributecardinality char(4) not null,
	attributeingroupcardinality char(4) not null,
	rulestrengthid bigint(20) not null,
	contenttypeid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_domainid(domainid),
	key idx_grouped(grouped),
	key idx_attributecardinality(attributecardinality),
	key idx_attributeingroupcardinality(attributeingroupcardinality),
	key idx_rulestrengthid(rulestrengthid),
	key idx_contenttypeid(contenttypeid)
) engine=myisam default charset=utf8;

drop table if exists mrcmModuleScopeRefset_f;
create table mrcmModuleScopeRefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mrcmrulerefsetid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mrcmrulerefsetid(mrcmrulerefsetid)
) engine=myisam default charset=utf8;

drop table if exists mrcmAttributeRangeRefset_f;
create table mrcmAttributeRangeRefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	rangeconstraint varchar(4000) not null,
	attributerule varchar(4000) not null,
	rulestrengthid bigint(20) not null,
	contenttypeid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_rulestrengthid(rulestrengthid),
	key idx_contenttypeid(contenttypeid)
) engine=myisam default charset=utf8;

drop table if exists mrcmDomainRefset_f;
create table mrcmDomainRefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainconstraint varchar(1024) not null,
	parentdomain varchar(1024) not null,
	proximalprimitiveconstraint varchar(4000) not null,
	proximalprimitiverefinement varchar(4000) not null,
	domaintemplateforprecoordination varchar(4000) not null,
	domaintemplateforpostcoordination varchar(4000) not null,
	guideurl varchar(255) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmAttributeDomainRefset_d;
create table mrcmAttributeDomainRefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainid bigint(20) not null,
	grouped char(1) not null,
	attributecardinality char(4) not null,
	attributeingroupcardinality char(4) not null,
	rulestrengthid bigint(20) not null,
	contenttypeid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_domainid(domainid),
	key idx_grouped(grouped),
	key idx_attributecardinality(attributecardinality),
	key idx_attributeingroupcardinality(attributeingroupcardinality),
	key idx_rulestrengthid(rulestrengthid),
	key idx_contenttypeid(contenttypeid)
) engine=myisam default charset=utf8;

drop table if exists mrcmModuleScopeRefset_d;
create table mrcmModuleScopeRefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mrcmrulerefsetid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mrcmrulerefsetid(mrcmrulerefsetid)
) engine=myisam default charset=utf8;

drop table if exists mrcmAttributeRangeRefset_d;
create table mrcmAttributeRangeRefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	rangeconstraint varchar(4000) not null,
	attributerule varchar(4000) not null,
	rulestrengthid bigint(20) not null,
	contenttypeid bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_rulestrengthid(rulestrengthid),
	key idx_contenttypeid(contenttypeid)
) engine=myisam default charset=utf8;

drop table if exists mrcmDomainRefset_d;
create table mrcmDomainRefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainconstraint varchar(1024) not null,
	parentdomain varchar(1024) not null,
	proximalprimitiveconstraint varchar(4000) not null,
	proximalprimitiverefinement varchar(4000) not null,
	domaintemplateforprecoordination varchar(4000) not null,
	domaintemplateforpostcoordination varchar(4000) not null,
	guideurl varchar(255) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;