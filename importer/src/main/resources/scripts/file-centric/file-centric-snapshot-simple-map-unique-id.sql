
/******************************************************************************** 
	file-centric-snapshot-simple-map-unique-id

	Assertion:
	ID is unique in the SIMPLEMAP REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('SM RS: id=',a.id, ':Non unique id in current SIMPLEMAP REFSET snapshot file.') 	
	from curr_simplemaprefset_s a	
	group by a.id
	having  count(a.id) > 1;