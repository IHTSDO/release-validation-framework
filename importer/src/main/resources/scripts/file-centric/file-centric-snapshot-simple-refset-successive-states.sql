
/******************************************************************************** 
	file-centric-snapshot-simple-refset-successive-states

	Assertion:	
	New inactive states follow active states in the SIMPLE REFSET snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding invalid inactive states*/
	create or replace view v_curr_snapshot as
	select a.id 
	from curr_simplerefset_s a , prev_simplerefset_s b
	where cast(a.effectivetime as datetime) =
				(select max(cast(effectivetime as datetime)) 
				 from curr_simplerefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Simple RS: id=',a.id, ':Invalid inactive states in the SIMPLE REFSET snapshot.') 	
	from v_curr_snapshot a;


	drop view v_curr_snapshot;
