
/******************************************************************************** 
	file-centric-snapshot-definition-unique-terms

	Assertion:
	There are no active duplicate Definition terms in the DEFINITION snapshot file.

********************************************************************************/
	
/* 	view of current snapshot made by finding duplicate terms in textdefinition file*/
	create or replace view v_curr_snapshot as
	select  a.term 
	from curr_textdefinition_s a
	where active = 1
	group by BINARY  a.term
	having count(a.term) > 1 ;

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TEXTDEF: Term=',a.term, ':There are no duplicate Definition terms in the DEFINITION snapshot file.') 	
	from v_curr_snapshot a;


	drop view v_curr_snapshot;

	