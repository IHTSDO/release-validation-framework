
/******************************************************************************** 
	file-centric-snapshot-concept-successive-states

	Assertion:	
	New inactive states follow active states in the CONCEPT snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding duplicate identifiers */
	create or replace view v_curr_snapshot as
	select a.id 
	from curr_concept_s a , prev_concept_s b
	where cast(a.effectivetime as datetime) =
				(select max(cast(effectivetime as datetime)) 
				 from curr_concept_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ':New inactive states follow active states in the CONCEPT snapshot.') 	
	from v_curr_snapshot a;


	drop view v_curr_snapshot;
