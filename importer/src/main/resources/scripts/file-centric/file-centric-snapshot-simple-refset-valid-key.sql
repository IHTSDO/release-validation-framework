
/******************************************************************************** 
	file-centric-snapshot-simple-refset-valid-key

	Assertion:
	There is a 1:1 relationship between the id and the key values in the SIMPLE REFSET snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by key values in SIMPLE REFSET */
	create or replace view v_curr_snapshot as
	select a.id , a.refsetid , a.referencedcomponentid 
	from curr_simplerefset_s a 
	group by a.id , a.refsetid , a.referencedcomponentid
	having count(a.id) > 1 and count(a.refsetid) > 1 and count(a.referencedcomponentid ) > 1;
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Simple RS: id=',a.id, ':Invalid keys in SIMPLE REFSET snapshot file.') 	
	from v_curr_snapshot a;

	drop view v_curr_snapshot;
	