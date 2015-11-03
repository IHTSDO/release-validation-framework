
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
		concat('Simple Refset: id=',a.id, ' has more than one set of immutable keys in the snapshot file.') 	
	from curr_simplerefset_s a 
	group by a.id , a.refsetid , a.referencedcomponentid
	having count(a.id) > 1 and count(a.refsetid) > 1 and count(a.referencedcomponentid ) > 1;