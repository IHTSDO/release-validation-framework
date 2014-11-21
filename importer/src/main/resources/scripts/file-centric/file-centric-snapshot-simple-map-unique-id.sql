
/******************************************************************************** 
	file-centric-snapshot-simple-map-unique-id

	Assertion:
	ID is unique in the SIMPLEMAP REFSET snapshot.

********************************************************************************/
	
/* view of current snapshot made by finding duplicate ids */
	create or replace view v_curr_snapshot as
	select a.id
	from curr_simplemaprefset_s a	
	group by a.id
	having  count(a.id) > 1;
	

	
/* inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('SM RS: id=',a.id, ':Non unique id in current SIMPLEMAP REFSET snapshot file.') 	
	from v_curr_snapshot a;


	drop view v_curr_snapshot;
