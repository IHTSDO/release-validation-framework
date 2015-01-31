
/******************************************************************************** 
	file-centric-snapshot-description-unique-id

	Assertion:
	The current Description snapshot file has unique ids.

********************************************************************************/
	
/* 	view of current snapshot made by finding duplicate identifiers */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id
	from curr_description_s a	
	group by a.id
	having  count(a.id) > 1;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: id=',a.id, ':Non unique id in description release file.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
