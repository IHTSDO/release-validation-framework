/******************************************************************************** 
	component-centric-snapshot-description-unique-fsn-case-insensitive-checking.sql
	Assertion:
	The FSN term should be unique in active content when case sensitivity is ingored.
	Note: The failures of this assertion are reported as warning not error because there are cases that are not considered as duplicate.
	(e.g Toxic effect of antimony and/or its compounds (disorder) is dupldate of Toxic effect of antimony AND/OR its compounds (disorder).
	However Blood group antibody I (substance) and Blood group antibody i (substance) are not duplicate.)
	The reason of using COLLATE utf8_general_ci is due to the collation of term in the description table
	is in binary which is case sensitive but for checking FSN uniquenss it requires case insensitive. 
********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.conceptid,
	concat('FSN=',a.term, ' concept=',a.conceptid, ': FSN term is not unique in description snapshot when case is ignored')
	from curr_description_d a,
	(select b.term, count(b.id) as total from curr_description_s b join curr_concept_s c on b.conceptid=c.id
	where b.active=1 and c.active=1 and b.typeid ='900000000000003001' group by term COLLATE utf8_general_ci having total > 1) result
	where a.term COLLATE utf8_general_ci = result.term;