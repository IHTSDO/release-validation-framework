
/******************************************************************************** 
	file-centric-snapshot-description-trim

	Assertion:
	No active Terms contain leading or trailing spaces.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's with leading and training spaces */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id from curr_description_s a 
	where a.active = 1
	and ( a.term != LTRIM(term) or a.term != RTRIM(term)); 
	


	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Description: id=',a.id, ' has active term with leading or trailing spaces.') 	
	from v_curr_snapshot a;
	
	drop table if exists v_curr_snapshot;
	