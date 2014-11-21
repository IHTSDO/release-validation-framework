
/******************************************************************************** 
	component-centric-snapshot-definition-uppercase

	Assertion:
	The first letter of the Term should be capitalized in Text-Definition.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's with leading and training spaces */
	create or replace view v_curr_snapshot as
	select SUBSTRING(a.term , 1, 1) as originalcase ,  UCASE(SUBSTRING(a.term , 1, 1)) as uppercase , a.term  
	from curr_textdefinition_s a ;
	 

/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TEXTDEF: term=',a.term, ':First letter of the Term not capitalized.') 	
	from v_curr_snapshot a
	where BINARY originalcase != uppercase;


	drop view v_curr_snapshot;
	