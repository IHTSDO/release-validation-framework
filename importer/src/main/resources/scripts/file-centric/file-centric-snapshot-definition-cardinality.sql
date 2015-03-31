
/******************************************************************************** 
	file-centric-snapshot-definition-cardinality.sql
	Assertion:
	There is at most one active definition per concept per language per dialect in the DEFINITION snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding more than one active definition per concept in the DEFINITION snapshot */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.conceptid
	from curr_textdefinition_s a	
	where a.active = 1
	group by a.conceptid,a.languagecode,binary a.term
	having  count(a.conceptid) > 1;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.conceptid, ' has more than one active definition per concept per dialect in the DEFINITION snapshot.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
