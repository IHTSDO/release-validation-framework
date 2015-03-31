
/******************************************************************************** 
	file-centric-snapshot-simple-refset-successive-states

	Assertion:	
	New inactive states for existing components must have active states in the previous SIMPLE REFSET snapshot

********************************************************************************/
	
/* 	view of current snapshot made by finding invalid inactive states*/
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id 
	from curr_simplerefset_s a , prev_simplerefset_s b
	where 
	a.active = '0'
	and b.active = '0'
	and a.id = b.id
	and a.effectivetime != b.effectivetime;
   	
	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Simple Refset: id=',a.id, ' should not have a new invalid inactive state in the SIMPLE REFSET snapshot.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
