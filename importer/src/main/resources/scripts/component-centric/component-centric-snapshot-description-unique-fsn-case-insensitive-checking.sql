/******************************************************************************** 
	component-centric-snapshot-description-unique-fsn-case-insensitive-checking.sql
	Assertion:
	The FSN term should be unique in active content when case sensitivity is ingored.
	Note: The failures of this assertion are reported as warning not error because there are cases that are not considered as duplicate.
	(e.g Toxic effect of antimony and/or its compounds (disorder) is dupldate of Toxic effect of antimony AND/OR its compounds (disorder).
	However Blood group antibody I (substance) and Blood group antibody i (substance) are not duplicate.)
	The reason to create fsn_edited and active_fsns temp tables due to the collation of term in the description tables
	is in binary which is case sensitive but for checking FSN uniquenss it requires case insensitive. 
********************************************************************************/
drop table if exists fsn_edited;
create table fsn_edited (conceptid bigint(20), term varchar(255),key idx_fsn_edited(term)) default charset=utf8;

insert into fsn_edited(conceptid,term) (select distinct b.conceptid,b.term from curr_concept_d a join curr_description_s b on a.id=b.conceptid
where a.active=1 and b.active=1 and b.typeid ='900000000000003001');

insert into fsn_edited(conceptid,term) (select distinct a.conceptid,a.term from curr_description_d a join curr_concept_s b on a.conceptid=b.id
where a.active=1 and b.active=1 and a.typeid ='900000000000003001' and not exists (select conceptid from fsn_edited c where c.conceptid=a.conceptid));

drop table if exists active_fsns;
create table active_fsns (conceptid bigint(20),term varchar(255),key idx_active_fsn(term)) default charset=utf8;	
	
insert into active_fsns(conceptid,term) (select distinct b.conceptid,b.term from curr_concept_s a join curr_description_s b on a.id=b.conceptid 
where a.active=1 and b.active=1 and b.typeid ='900000000000003001');

insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.conceptid,
	concat('FSN=',a.term, ' concept=',a.conceptid, ': FSN term is not unique in description snapshot when case is ignored')
	from fsn_edited a where exists (select count(*) total from active_fsns b where a.term = b.term having total >1)
	and not exists ( select count(*) total from curr_description_s c where c.term = a.term having total >1);
