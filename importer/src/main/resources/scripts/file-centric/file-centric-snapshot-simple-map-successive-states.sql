
/******************************************************************************** 
	file-centric-snapshot-simple-map-successive-states

	Assertion:	
	New inactive states follow active states in the SIMPLE MAP REFSET snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding invalid inactive states*/
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id 
	from curr_simplemaprefset_s a , prev_simplemaprefset_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_simplemaprefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('SM RS: id=',a.id, ':Invalid inactive states in the SIMPLE MAP REFSET snapshot.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
