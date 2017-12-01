
/******************************************************************************** 
	file-centric-snapshot-simple-refset-valid-key

	Assertion:
	There is a 1:1 relationship between the id and the key values in the SIMPLE REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Refset id:',a.refsetid, ' and referencedcomponent Id:', a.referencedcomponentid, ' are duplicated in the simple refset snapshot file.') 	
	from curr_simplerefset_s a 
	group by a.refsetid , a.referencedcomponentid
	having count(a.id) > 1;