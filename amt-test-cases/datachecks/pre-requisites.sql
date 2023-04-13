DELIMITER //

SET default_storage_engine=MYISAM//

DROP TABLE IF EXISTS concept_active//
CREATE TABLE concept_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS
SELECT * FROM concept_s AS concepts WHERE active = 1//
create unique index concept_active_id_ix on concept_active(id)//
create index concept_active_effectivetime_ix on concept_active(effectivetime)//
create index concept_active_definitionstatusid_ix on concept_active(definitionstatusid)//
create index concept_active_moduleid_ix on concept_active(moduleid)//
create index concept_active_active_ix on concept_active(active)//

DROP TABLE IF EXISTS description_active//
CREATE TABLE description_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS
SELECT * FROM description_s AS descriptions WHERE active = 1//
create unique index description_active_id_ix on description_active(id)//
create index description_active_casesignificanceid_ix on description_active(casesignificanceid)//
create index description_active_conceptid_ix on description_active(conceptid)//
create index description_active_effectivetime_ix on description_active(effectivetime)//
create index description_active_moduleid_ix on description_active(moduleid)//
create index description_active_term_ix on description_active(term(256))//
create fulltext index description_active_term_ftix on description_active(term(256))//
create index description_active_typeid_ix on description_active(typeid)//
create index description_active_active_ix on description_active(active)//
create index description_active_languagecode_ix on description_active(languagecode)//

DROP TABLE IF EXISTS relationship_active//
CREATE TABLE relationship_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS
SELECT * FROM relationship_s AS relationships WHERE active = 1//
Create unique index relationship_active_id_idx on relationship_active(id)//
Create index relationship_active_effectivetime_idx on relationship_active(effectivetime)//
Create index relationship_active_moduleid_idx on relationship_active(moduleid)//
Create index relationship_active_sourceid_idx on relationship_active(sourceid)//
Create index relationship_active_destinationid_idx on relationship_active(destinationid)//
Create index relationship_active_relationshipgroup_idx on relationship_active(relationshipgroup)//
Create index relationship_active_typeid_idx on relationship_active(typeid)//
Create index relationship_active_characteristictypeid_idx on relationship_active(characteristictypeid)//
Create index relationship_active_modifierid_idx on relationship_active(modifierid)//
Create index relationship_active_active_idx on relationship_active(active)//

DROP TABLE IF EXISTS  simplerefset_active//
CREATE TABLE simplerefset_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS
SELECT * FROM simplerefset_s refset WHERE ACTIVE = 1//
create unique index simplerefset_active_id_ix on simplerefset_active(id)//
create index simplerefset_active_effectivetime_ix on simplerefset_active(effectivetime)//
create index simplerefset_active_moduleid_ix on simplerefset_active(moduleid)//
create index simplerefset_active_referencedComponentId_ix on simplerefset_active(referencedComponentId)//
create index simplerefset_active_refsetid_ix on simplerefset_active(refsetid)//
create index simplerefset_active_active_ix on simplerefset_active(active)//

DROP TABLE IF EXISTS relationship_concrete_values_active//
CREATE TABLE relationship_concrete_values_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS
SELECT * FROM relationship_concrete_values_s AS relationships WHERE active = 1//
Create unique index relconvals_active_id_idx on relationship_concrete_values_active(id)//
Create index relconvals_active_effectivetime_idx on relationship_concrete_values_active(effectivetime)//
Create index relconvals_active_moduleid_idx on relationship_concrete_values_active(moduleid)//
Create index relconvals_active_sourceid_idx on relationship_concrete_values_active(sourceid)//
Create index relconvals_active_value_idx on relationship_concrete_values_active(value)//
Create index relconvals_active_relationshipgroup_idx on relationship_concrete_values_active(relationshipgroup)//
Create index relconvals_active_typeid_idx on relationship_concrete_values_active(typeid)//
Create index relconvals_active_characteristictypeid_idx on relationship_concrete_values_active(characteristictypeid)//
Create index relconvals_active_modifierid_idx on relationship_concrete_values_active(modifierid)//
Create index relconvals_active_active_idx on relationship_concrete_values_active(active)//

drop table if exists langrefset_active//
CREATE TABLE langrefset_active ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AS
SELECT * FROM langrefset_s where active = 1//
create index langrefset_active_idx_acceptabilityid on langrefset_active (acceptabilityid)//
create index langrefset_active_idx_active on langrefset_active (active)//
create index langrefset_active_idx_effectivetime on langrefset_active (effectivetime)//
create index langrefset_active_idx_id on langrefset_active (id)//
create index langrefset_active_idx_moduleid on langrefset_active (moduleid)//
create index langrefset_active_idx_referencedcomponentid on langrefset_active (referencedcomponentid)//
create index langrefset_active_idx_refsetid on langrefset_active (refsetid)//

-- Create a bunch of functions
drop procedure if exists TableIsPopulated//
create procedure TableIsPopulated(IN tablename varchar(100))
BEGIN SET @s = CONCAT('SELECT COUNT(1) FROM ',tablename); PREPARE stmt FROM @s; EXECUTE stmt; END//

drop function if exists get_cr_ADRS_PT//
create function get_cr_ADRS_PT(candidate bigint) returns varchar(4500)
BEGIN RETURN (SELECT da.term FROM langrefset_active as lr join description_active as da
                                                               on da.id = lr.referencedcomponentid WHERE lr.acceptabilityid = 900000000000548007 and conceptid = candidate); END//

drop function if exists get_cr_FSN//
create function get_cr_FSN(candidate bigint) returns varchar(4500)
BEGIN RETURN (SELECT term FROM description_active where conceptId = candidate and typeId = 900000000000003001 and languageCode = 'en'); END//

drop function if exists get_cr_PercentDefined//
create function get_cr_PercentDefined(refset bigint) returns decimal(6, 4)
BEGIN SET @refset = refset; SET @refsetSize = (select count(1) from simplerefset_active where refsetId = @refset); SET @definedCount = (select count(1) from concept_active where definitionStatusId = 900000000000073002 and id in (select referencedComponentId from simplerefset_active where refsetId = @refset));  RETURN CONVERT(@definedCount/ @refsetSize * 100,DECIMAL(6,4)); END//

drop function if exists isActiveConcept_cr//
create function isActiveConcept_cr(candidate varchar(20)) returns tinyint(1)
BEGIN  RETURN (select count(1) from concept_active where id = candidate);  END//

drop function if exists isActiveMemberOf_cr_refset//
create function isActiveMemberOf_cr_refset(candidate bigint, targetRefset bigint) returns int
BEGIN RETURN (select count(1) from simplerefset_active where refsetId = targetRefset and referencedComponentId = candidate); END//

-- drop function if exists isAncestorOf_cr//
-- create function isAncestorOf_cr(candidate bigint, target bigint) returns int
-- BEGIN RETURN (select count(1) from rf2_cr_TransitiveClosureTable where destinationId = candidate AND sourceId = target); END//

drop function if exists isChildOf_cr//
create function isChildOf_cr(candidate bigint, target bigint) returns int
BEGIN RETURN (select count(1) from relationship_active where sourceId = candidate AND destinationId = target AND typeId = 116680003); END//

-- drop function if exists isDescendentOf_cr//
-- create function isDescendentOf_cr(candidate bigint, target bigint) returns int
-- BEGIN RETURN (select count(1) from rf2_cr_TransitiveClosureTable where sourceId = candidate AND destinationId = target); END//

-- drop function if exists isKindOf_cr//
-- create function isKindOf_cr(candidate bigint, target bigint) returns int
-- BEGIN RETURN isDescendentOf_cr(candidate,target)+isConcept_cr(candidate,target); END//

drop function if exists isPositiveInteger//
create function isPositiveInteger(candidate varchar(20)) returns tinyint(1)
BEGIN CASE WHEN (SIGN(candidate) != 1) THEN RETURN FALSE; WHEN (ROUND(candidate) != candidate) THEN RETURN FALSE; ELSE RETURN TRUE; END CASE; END//

drop function if exists isValidComponentId_cr//
create function isValidComponentId_cr(candidate varchar(20)) returns tinyint(1)
BEGIN declare x int;  IF (CHAR_LENGTH(candidate) <19) THEN CASE SUBSTRING(candidate, -2, 1) WHEN 0 THEN SET x = (select count(1) from concept_s where id = candidate); WHEN 1 THEN	SET x = (select count(1) from (select * from description_s where id = candidate UNION select * from textdefinition_s where id = candidate) AS D); WHEN 2 THEN SET x = (select count(1) from relationship_s where id = candidate); ELSE RETURN FALSE; END CASE; ELSE return false; END IF;  IF x > 0 THEN return true; ELSE return false; END IF;   END//

-- drop function if exists isValidModuleId//
-- create function isValidModuleId(candidate varchar(20)) returns tinyint(1)
-- BEGIN  RETURN isDescendentOf_cr(candidate, 900000000000443000);  END//

-- drop function if exists isValidRefsetId_cr//
-- create function isValidRefsetId_cr(candidate varchar(20)) returns tinyint(1)
-- BEGIN  RETURN isDescendentOf_cr(candidate, 900000000000455006);  END//

drop function if exists isValidTimeFormat//
create function isValidTimeFormat(candidate varchar(30)) returns tinyint(1)
BEGIN  declare shortDate varchar(30); SET shortDate = DATE_FORMAT(candidate,'%Y%m%d');  CASE WHEN (CHAR_LENGTH(shortDate) != 8) THEN RETURN FALSE; WHEN (SUBSTRING(shortDate,1,4) < 2002 || SUBSTRING(shortDate,1,4) > 2099) THEN RETURN FALSE; WHEN (SUBSTRING(shortDate,5,2) < 1 || SUBSTRING(shortDate,5,2) > 12) THEN RETURN FALSE; WHEN (SUBSTRING(shortDate,7,2) < 1 || SUBSTRING(shortDate,7,2) > 31) THEN RETURN FALSE; WHEN (NOT isPositiveInteger(shortDate)) THEN RETURN FALSE; ELSE RETURN TRUE; END CASE;  END//

drop function if exists isValidUUID//
create function isValidUUID(candidate varchar(40)) returns tinyint(1)
BEGIN IF (LENGTH(candidate) != 36) THEN RETURN FALSE; ELSEIF (SUBSTRING(candidate,9,1) != '-') THEN RETURN FALSE; ELSEIF (SUBSTRING(candidate,14,1) != '-') THEN RETURN FALSE; ELSEIF (SUBSTRING(candidate,19,1) != '-') THEN RETURN FALSE; ELSEIF (SUBSTRING(candidate,24,1) != '-') THEN RETURN FALSE; ELSE RETURN TRUE; END IF;  END//

