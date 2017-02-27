
/******************************************************************************** 
	file-centric-snapshot-simple-refset-valid-referencedcomponentid.sql

	Assertion:
	Referencedcomponentid refers to valid concepts in the Simple Refset snapshot file.

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Simple RefSet:',a.referencedcomponentid, ':Invalid Referencedcomponentid in Simple Refset snapshot.') 	
	from curr_simplerefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where a.active=1 and b.id is null and not exists ( select id from curr_description_s where id=a.referencedcomponentid);