
/******************************************************************************** 
	file-centric-snapshot-association-successive-states

	Assertion:	
	New inactive states follow active states in the ASSOCIATION REFSET snapshot file.

********************************************************************************/
	
/* 	view of current snapshot made by finding invalid inactive states*/
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id 
	from curr_associationrefset_s a , prev_associationrefset_s b
	where cast(a.effectivetime as datetime) =
				(select max(cast(effectivetime as datetime)) 
				 from curr_associationrefset_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOC RS: id=',a.id, ':Invalid inactive states in the ASSOCIATION REFSET snapshot file.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
