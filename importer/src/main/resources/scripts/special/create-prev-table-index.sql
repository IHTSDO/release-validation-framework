drop table if exists prev_concept_f;
create table prev_concept_f(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
definitionstatusid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_definitionstatusid(definitionstatusid)
) engine=myisam default charset=utf8;


drop table if exists prev_description_f;
create table prev_description_f(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
conceptid bigint not null,
languagecode varchar(2) not null,
typeid bigint not null,
term varchar(255) not null,
casesignificanceid bigint not null,
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

drop table if exists prev_textdefinition_f;
create table prev_textdefinition_f(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
conceptid bigint not null,
languagecode varchar(2) not null,
typeid bigint not null,
term varchar(1024) not null,
casesignificanceid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists prev_relationship_f;
create table prev_relationship_f(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
sourceid bigint not null,
destinationid bigint not null,
relationshipgroup bigint not null,
typeid bigint not null,
characteristictypeid bigint not null,
modifierid bigint not null,
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

drop table if exists prev_stated_relationship_f;
create table prev_stated_relationship_f(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
sourceid bigint not null,
destinationid bigint not null,
relationshipgroup bigint not null,
typeid bigint not null,
characteristictypeid bigint not null,
modifierid bigint not null,
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

drop table if exists prev_langrefset_f;
create table prev_langrefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
acceptabilityid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_acceptabilityid(acceptabilityid)
) engine=myisam default charset=utf8;

drop table if exists prev_associationrefset_f;
create table prev_associationrefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
targetcomponentid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_targetcomponentid(targetcomponentid)
) engine=myisam default charset=utf8;

drop table if exists prev_attributevaluerefset_f;
create table prev_attributevaluerefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
valueid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_valueid(valueid)
) engine=myisam default charset=utf8;

drop table if exists prev_simplemaprefset_f;
create table prev_simplemaprefset_f(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
maptarget varchar(32) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_maptarget(maptarget)
) engine=myisam default charset=utf8;

drop table if exists prev_simplerefset_f;
create table prev_simplerefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint not null,
	refsetid bigint not null,
	referencedcomponentid bigint not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists prev_complexmaprefset_f;
create table prev_complexmaprefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint not null,
	refsetid bigint not null,
	referencedcomponentid bigint not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule bigint,
	mapAdvice bigint,
	mapTarget bigint,
	correlationId bigint not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;






drop table if exists prev_concept_s;
create table prev_concept_s(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
definitionstatusid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_definitionstatusid(definitionstatusid)
) engine=myisam default charset=utf8;


drop table if exists prev_description_s;
create table prev_description_s(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
conceptid bigint not null,
languagecode varchar(2) not null,
typeid bigint not null,
term varchar(255) not null,
casesignificanceid bigint not null,
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

drop table if exists prev_textdefinition_s;
create table prev_textdefinition_s(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
conceptid bigint not null,
languagecode varchar(2) not null,
typeid bigint not null,
term varchar(1024) not null,
casesignificanceid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists prev_relationship_s;
create table prev_relationship_s(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
sourceid bigint not null,
destinationid bigint not null,
relationshipgroup bigint not null,
typeid bigint not null,
characteristictypeid bigint not null,
modifierid bigint not null,
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

drop table if exists prev_stated_relationship_s;
create table prev_stated_relationship_s(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
sourceid bigint not null,
destinationid bigint not null,
relationshipgroup bigint not null,
typeid bigint not null,
characteristictypeid bigint not null,
modifierid bigint not null,
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

drop table if exists prev_langrefset_s;
create table prev_langrefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
acceptabilityid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_acceptabilityid(acceptabilityid)
) engine=myisam default charset=utf8;

drop table if exists prev_associationrefset_s;
create table prev_associationrefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
targetcomponentid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_targetcomponentid(targetcomponentid)
) engine=myisam default charset=utf8;

drop table if exists prev_attributevaluerefset_s;
create table prev_attributevaluerefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
valueid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_valueid(valueid)
) engine=myisam default charset=utf8;

drop table if exists prev_simplemaprefset_s;
create table prev_simplemaprefset_s(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
maptarget varchar(32) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_maptarget(maptarget)
) engine=myisam default charset=utf8;

drop table if exists prev_simplerefset_s;
create table prev_simplerefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint not null,
	refsetid bigint not null,
	referencedcomponentid bigint not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists prev_complexmaprefset_s;
create table prev_complexmaprefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint not null,
	refsetid bigint not null,
	referencedcomponentid bigint not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule bigint,
	mapAdvice bigint,
	mapTarget bigint,
	correlationId bigint not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;




drop table if exists prev_concept_d;
create table prev_concept_d(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
definitionstatusid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_definitionstatusid(definitionstatusid)
) engine=myisam default charset=utf8;


drop table if exists prev_description_d;
create table prev_description_d(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
conceptid bigint not null,
languagecode varchar(2) not null,
typeid bigint not null,
term varchar(255) not null,
casesignificanceid bigint not null,
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

drop table if exists prev_textdefinition_d;
create table prev_textdefinition_d(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
conceptid bigint not null,
languagecode varchar(2) not null,
typeid bigint not null,
term varchar(1024) not null,
casesignificanceid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;

drop table if exists prev_relationship_d;
create table prev_relationship_d(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
sourceid bigint not null,
destinationid bigint not null,
relationshipgroup bigint not null,
typeid bigint not null,
characteristictypeid bigint not null,
modifierid bigint not null,
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

drop table if exists prev_stated_relationship_d;
create table prev_stated_relationship_d(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
sourceid bigint not null,
destinationid bigint not null,
relationshipgroup bigint not null,
typeid bigint not null,
characteristictypeid bigint not null,
modifierid bigint not null,
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

drop table if exists prev_langrefset_d;
create table prev_langrefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
acceptabilityid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_acceptabilityid(acceptabilityid)
) engine=myisam default charset=utf8;

drop table if exists prev_associationrefset_d;
create table prev_associationrefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
targetcomponentid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_targetcomponentid(targetcomponentid)
) engine=myisam default charset=utf8;

drop table if exists prev_attributevaluerefset_d;
create table prev_attributevaluerefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
valueid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_valueid(valueid)
) engine=myisam default charset=utf8;

drop table if exists prev_simplemaprefset_d;
create table prev_simplemaprefset_d(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
maptarget varchar(32) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_maptarget(maptarget)
) engine=myisam default charset=utf8;

drop table if exists prev_simplerefset_d;
create table prev_simplerefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint not null,
	refsetid bigint not null,
	referencedcomponentid bigint not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists prev_complexmaprefset_d;
create table prev_complexmaprefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint not null,
	refsetid bigint not null,
	referencedcomponentid bigint not null,
	mapGroup smallint not null,
	mapPriority smallint not null,
	mapRule bigint,
	mapAdvice bigint,
	mapTarget bigint,
	correlationId bigint not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_mapTarget(mapTarget)
) engine=myisam default charset=utf8;
