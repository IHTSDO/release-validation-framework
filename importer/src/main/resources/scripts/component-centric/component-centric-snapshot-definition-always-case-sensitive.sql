
/******************************************************************************** 
	component-centric-snapshot-definition-always-case-sensitive.sql

	Assertion:
	All definitions are case sensitive.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's with leading and training spaces */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id 
	from curr_textdefinition_s a 
	where a.casesignificanceid != '900000000000017005';
	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TEXTDEF: id=',a.id, ':Definitions are not case sensitive.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
	