
/******************************************************************************** 
	file-centric-snapshot-simple-map-valid-key

	Assertion:
	There is a 1:1 relationship between the id and the key values in the simple map snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('referencedcomponentId:', a.referencedcomponentid, ' refsetId:', a.refsetid, ' mapTarget:', a.maptarget, ' are duplicated in the simple map fefset snapshot file.') 	
	from curr_simplemaprefset_s a 
	group by a.refsetid, a.referencedcomponentid, a.maptarget
	having count(a.id) > 1;
	