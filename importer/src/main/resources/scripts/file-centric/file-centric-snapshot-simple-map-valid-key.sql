
/******************************************************************************** 
	file-centric-snapshot-simple-map-valid-key

	Assertion:
	There is a 1:1 relationship between the id and the key values in the SIMPLE MAP REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('SM RS: id=',a.id, ':Invalid keys in SIMPLE MAP REFSET snapshot file.') 	
	from curr_simplemaprefset_s a 
	group by a.id , a.refsetid , a.referencedcomponentid 
	having count(a.id) > 1 and count(a.refsetid) > 1 and count(a.referencedcomponentid ) > 1;
	