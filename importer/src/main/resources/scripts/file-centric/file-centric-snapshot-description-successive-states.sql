
/******************************************************************************** 
	file-centric-snapshot-description-successive-states

	Assertion:	
	New inactive states follow active states in the DESCRIPTION snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding inactive states follows active states */
	create or replace view v_curr_snapshot as
	select a.id 
	from curr_description_s a , prev_description_s b
	where cast(a.effectivetime as datetime) =
				(select max(cast(effectivetime as datetime)) 
				 from curr_description_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: id=',a.id, ':New inactive states follow active states in the DESCRIPTION snapshot.') 	
	from v_curr_snapshot a;


	drop view v_curr_snapshot;
