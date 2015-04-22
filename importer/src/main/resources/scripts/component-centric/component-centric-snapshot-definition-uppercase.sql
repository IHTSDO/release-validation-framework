
/******************************************************************************** 
	component-centric-snapshot-definition-uppercase

	Assertion:
	The first letter of the Term should be capitalized in Text-Definition.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's with leading and training spaces */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select SUBSTRING(a.term , 1, 1) as originalcase ,  UCASE(SUBSTRING(a.term , 1, 1)) as uppercase , a.id 
	from curr_textdefinition_s a where a.active =1;
	 

/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TEXTDEFINITION: id=',a.id, ' contains a term that the first letter is not capitalized.') 	
	from v_curr_snapshot a
	where BINARY originalcase != uppercase;


	drop table if exists v_curr_snapshot;
	