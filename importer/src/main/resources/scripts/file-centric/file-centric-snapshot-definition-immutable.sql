
/******************************************************************************** 
	file-centric-snapshot-definition-immutable

	Assertion:
	There is a 1:1 relationship between the ID and the immutable values in DEFINITION snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by immutable values in DEFINITION */
	create or replace view v_curr_snapshot as
	select a.id , a.typeid , a.languagecode , a.conceptid 
	from curr_textdefinition_s a 
	group by a.id , a.typeid , a.languagecode , a.conceptid
	having count(a.id) > 1 and count(a.typeid ) > 1 and count(languagecode) > 1 and count(conceptid) > 1;
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ':There is a 1:1 relationship between the id and the immutable values in definition snapshot.') 	
	from v_curr_snapshot a;

	drop view v_curr_snapshot;
	