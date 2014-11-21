
/******************************************************************************** 
	file-centric-snapshot-description-double-spaces

	Assertion:
	No active Terms associated with active concepts contain double spaces.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's without semantic tags */
	create or replace view v_curr_snapshot as
	select a.term 
	from curr_description_s a , curr_concept_s b
	where a.active = 1
	and a.conceptid = b.id
	and b.active = 1
	and a.term like '%  %'; 	
	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: Term=',a.term, ':Active Terms containing double spaces.') 	
	from v_curr_snapshot a;


	drop view v_curr_snapshot;

	