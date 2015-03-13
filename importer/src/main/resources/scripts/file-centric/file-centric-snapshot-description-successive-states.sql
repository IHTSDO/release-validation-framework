
/******************************************************************************** 
	file-centric-snapshot-description-successive-states

	Assertion:	
	New inactive states must follow active states in the DESCRIPTION snapshot.
	Note: Unless there are changes in other fields since last release due to data correction in current release 

********************************************************************************/
	
/* 	view of current snapshot made by finding inactive states follows active states */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id 
	from curr_description_s a , prev_description_s b	
	where cast(a.effectivetime as datetime) >=
				(select min(cast(effectivetime as datetime)) 
				 from curr_description_d)
	and a.active = '0'
	and b.active = '0'
	and a.id = b.id
	and a.moduleid =b.moduleid
	and a.typeid = b.typeid
	and a.casesignificanceid = b.casesignificanceid
	and a.effectivetime != b.effectivetime;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: id=',a.id, ': is inactive in current release but not active in previous DESCRIPTION snapshot.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
