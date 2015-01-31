
/******************************************************************************** 
	file-centric-snapshot-concept-unique-id

	Assertion:
	There are zero active definitions or there is one active definition per concept in the DEFINITION snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding more than one active definition per concept in the DEFINITION snapshot */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.conceptid
	from curr_textdefinition_s a	
	where a.active = 1
	group by a.conceptid
	having  count(a.conceptid) > 1;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.conceptid, ':More than one active definition per concept in the DEFINITION snapshot.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
