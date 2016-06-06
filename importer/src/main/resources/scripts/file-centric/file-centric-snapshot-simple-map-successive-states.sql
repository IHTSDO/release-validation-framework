
/******************************************************************************** 
	file-centric-snapshot-simple-map-successive-states

	Assertion:	
	New inactive states follow active states in the SIMPLE MAP REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('SimpleMap Refset: id=',a.id, ' should not have a new inactive state as it was inactive previously.') 	
	from curr_simplemaprefset_s a , prev_simplemaprefset_s b
	where a.active = 0
	and a.id = b.id
	and a.active = b.active
	and a.effectivetime != b.effectivetime;

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('SimpleMap Refset: id=',a.id, ' is inactive but no active state found in the previous snapshot.') 	
	from curr_simplemaprefset_s a left join prev_simplemaprefset_s b
	on a.id=b.id
	where a.active = '0'
	and b.id is null;