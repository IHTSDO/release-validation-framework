
/******************************************************************************** 
	file-centric-snapshot-association-valid-key

	Assertion:
	There is a 1:1 relationship between the id and the key values in the ASSOCIATION REFSET snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ReferencedComponentId:', a.referencedcomponentid, ' targetComponentId:', a.targetcomponentid, ' and refsetId:', a.refsetid, ' are duplicated in the association refset snapshot file.') 	
	from curr_associationrefset_s a 
	group by a.refsetid , a.referencedcomponentid , a.targetcomponentid
	having count(id) > 1;
	
	