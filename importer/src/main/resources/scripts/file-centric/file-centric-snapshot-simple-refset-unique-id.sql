
/******************************************************************************** 
	file-centric-snapshot-simple-refset-unique-id

	Assertion:
	ID is unique in the SIMPLE REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Simple RS: id=',a.id, ':Non unique id in current SIMPLE REFSET snapshot file.') 	
	from curr_simplerefset_s a	
	group by a.id
	having  count(a.id) > 1;
