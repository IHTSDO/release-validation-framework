
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
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_description_s)
	and a.active = '0'
	and b.active = '0'
	and a.id = b.id
	and a.moduleid =b.moduleid
	and a.typeid = b.typeid
	and a.casesignificanceid = b.casesignificanceid;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESCRIPTION: id=',a.id, ' should not have a new inactive state in the current release as it was already inactive in previous snapshot.') 	
	from v_curr_snapshot a;

	drop table if exists v_curr_snapshot;
