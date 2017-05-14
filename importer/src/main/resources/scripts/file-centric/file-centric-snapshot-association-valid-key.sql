
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
		concat('ReferencedComponent: id=',a.referencedcomponentid, ' and targetComponentId:',a.targetcomponentid,
		' contains multiple association refset memember ids.') 	
	from curr_associationrefset_s a 
	group by a.id,a.refsetid , a.referencedcomponentid , a.targetcomponentid
	having count(a.id) >1 or count(a.refsetid) > 1 or count(a.referencedcomponentid ) > 1 or count(a.targetcomponentid) > 1;
	commit;
	