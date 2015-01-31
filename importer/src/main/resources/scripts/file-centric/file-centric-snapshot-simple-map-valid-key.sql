
/******************************************************************************** 
	file-centric-snapshot-simple-map-valid-key

	Assertion:
	There is a 1:1 relationship between the id and the key values in the SIMPLE MAP REFSET snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by key values in SIMPLE MAP REFSET */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id , a.refsetid , a.referencedcomponentid 
	from curr_simplemaprefset_s a 
	group by a.id , a.refsetid , a.referencedcomponentid 
	having count(a.id) > 1 and count(a.refsetid) > 1 and count(a.referencedcomponentid ) > 1;
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('SM RS: id=',a.id, ':Invalid keys in SIMPLE MAP REFSET snapshot file.') 	
	from v_curr_snapshot a;

	drop table if exists v_curr_snapshot;
	