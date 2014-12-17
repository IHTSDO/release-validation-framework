
/******************************************************************************** 
	file-centric-snapshot-description-immutable

	Assertion:
	There is a 1:1 relationship between the id and the immutable values in DESCRIPTION snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by immutable values in DESCRIPTION */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id , a.typeid , a.languagecode , a.conceptid 
	from curr_description_s a 
	group by a.id , a.typeid , a.languagecode , a.conceptid
	having count(a.id) > 1 and count(a.typeid ) > 1 and count(languagecode) > 1 and count(conceptid) > 1;
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: id=',a.id, ':There is a 1:1 relationship between the id and the immutable values in DESCRIPTION snapshot.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;

	