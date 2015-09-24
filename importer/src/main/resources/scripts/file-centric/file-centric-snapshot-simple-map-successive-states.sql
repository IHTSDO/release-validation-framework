
/******************************************************************************** 
	file-centric-snapshot-simple-map-successive-states

	Assertion:	
	New inactive states follow active states in the SIMPLE MAP REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('SM RS: id=',a.id, ':Invalid inactive states in the SIMPLE MAP REFSET snapshot.') 	
	from curr_simplemaprefset_s a , prev_simplemaprefset_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_simplemaprefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
