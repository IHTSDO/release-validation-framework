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
term varchar(4096) not null,
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

drop table if exists relationship_concrete_values_f;
create table relationship_concrete_values_f(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
value varchar(4096) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
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
	mapAdvice varchar(500),
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

drop table if exists moduledependencyrefset_f;
create table moduledependencyrefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	sourceeffectivetime char(8) not null,
	targeteffectivetime char(8) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_sourceeffectivetime(sourceeffectivetime),
	key idx_targeteffectivetime(targeteffectivetime)
) engine=myisam default charset=utf8;

drop table if exists owlexpressionrefset_f;
create table owlexpressionrefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	owlexpression text not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmattributedomainrefset_f;
create table mrcmattributedomainrefset_f(
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

drop table if exists mrcmmodulescoperefset_f;
create table mrcmmodulescoperefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mrcmRuleRefsetId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmattributerangerefset_f;
create table mrcmattributerangerefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	rangeConstraint text not null,
	attributeRule text not null,
	ruleStrengthId bigint(20) not null,
	contentTypeId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmdomainrefset_f;
create table mrcmdomainrefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainconstraint varchar(1024) not null,
	parentdomain varchar(1024) not null,
	proximalPrimitiveConstraint text not null,
	proximalPrimitiveRefinement text not null,
	domainTemplateForPrecoordination text not null,
	domainTemplateForPostcoordination text not null,
	guideurl varchar(255) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;


drop table if exists descriptiontyperefset_f;
create table descriptiontyperefset_f(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	descriptionformat bigint(20) not null,
	descriptionlength int not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_descriptionformat(descriptionformat),
	key idx_descriptionlength(descriptionlength)
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
term varchar(2048) not null,
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

drop table if exists relationship_concrete_values_s;
create table relationship_concrete_values_s(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
value varchar(4096) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
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
	mapAdvice varchar(500),
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

drop table if exists moduledependencyrefset_s;
create table moduledependencyrefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	sourceeffectivetime char(8) not null,
	targeteffectivetime char(8) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_sourceeffectivetime(sourceeffectivetime),
	key idx_targeteffectivetime(targeteffectivetime)
) engine=myisam default charset=utf8;

drop table if exists owlexpressionrefset_s;
create table owlexpressionrefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	owlexpression text not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmattributedomainrefset_s;
create table mrcmattributedomainrefset_s(
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

drop table if exists mrcmmodulescoperefset_s;
create table mrcmmodulescoperefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mrcmRuleRefsetId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmattributerangerefset_s;
create table mrcmattributerangerefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	rangeConstraint text not null,
	attributeRule text not null,
	ruleStrengthId bigint(20) not null,
	contentTypeId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmdomainrefset_s;
create table mrcmdomainrefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainconstraint varchar(1024) not null,
	parentdomain varchar(1024) not null,
	proximalPrimitiveConstraint text not null,
	proximalPrimitiveRefinement text not null,
	domainTemplateForPrecoordination text not null,
	domainTemplateForPostcoordination text not null,
	guideurl varchar(255) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists descriptiontyperefset_s;
create table descriptiontyperefset_s(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	descriptionformat bigint(20) not null,
	descriptionlength int not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_descriptionformat(descriptionformat),
	key idx_descriptionlength(descriptionlength)
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
term varchar(2048) not null,
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

drop table if exists relationship_concrete_values_d;
create table relationship_concrete_values_d(
id bigint(20) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint(20) not null,
sourceid bigint(20) not null,
value varchar(4096) not null,
relationshipgroup bigint(20) not null,
typeid bigint(20) not null,
characteristictypeid bigint(20) not null,
modifierid bigint(20) not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_sourceid(sourceid),
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
	mapAdvice varchar(500),
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
	mapAdvice varchar(500),
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
	mapAdvice varchar(500),
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
	mapAdvice varchar(500),
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


drop table if exists moduledependencyrefset_d;
create table moduledependencyrefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	sourceeffectivetime char(8) not null,
	targeteffectivetime char(8) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_sourceeffectivetime(sourceeffectivetime),
	key idx_targeteffectivetime(targeteffectivetime)
) engine=myisam default charset=utf8;

drop table if exists owlexpressionrefset_d;
create table owlexpressionrefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	owlexpression text not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmattributedomainrefset_d;
create table mrcmattributedomainrefset_d(
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

drop table if exists mrcmmodulescoperefset_d;
create table mrcmmodulescoperefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	mrcmRuleRefsetId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmattributerangerefset_d;
create table mrcmattributerangerefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	rangeConstraint text not null,
	attributeRule text not null,
	ruleStrengthId bigint(20) not null,
	contentTypeId bigint(20) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists mrcmdomainrefset_d;
create table mrcmdomainrefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	domainconstraint varchar(1024) not null,
	parentdomain varchar(1024) not null,
	proximalPrimitiveConstraint text not null,
	proximalPrimitiveRefinement text not null,
	domainTemplateForPrecoordination text not null,
	domainTemplateForPostcoordination text not null,
	guideurl varchar(255) not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid)
) engine=myisam default charset=utf8;

drop table if exists descriptiontyperefset_d;
create table descriptiontyperefset_d(
	id varchar(36) not null,
	effectivetime char(8) not null,
	active char(1) not null,
	moduleid bigint(20) not null,
	refsetid bigint(20) not null,
	referencedcomponentid bigint(20) not null,
	descriptionformat bigint(20) not null,
	descriptionlength int not null,
	key idx_id(id),
	key idx_effectivetime(effectivetime),
	key idx_active(active),
	key idx_moduleid(moduleid),
	key idx_refsetid(refsetid),
	key idx_referencedcomponentid(referencedcomponentid),
	key idx_descriptionformat(descriptionformat),
	key idx_descriptionlength(descriptionlength)
) engine=myisam default charset=utf8;

