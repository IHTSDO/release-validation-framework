
/******************************************************************************** 
	file-centric-snapshot-map-correlation-origin-unique-id.sql
	Assertion:
	ID is unique in the map correlation origin refset snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('id=',a.id, ':Non unique id in the MapCorrelationOrigin Snapshot file.') 	
	from curr_mapCorrelationOriginRefset_s a
	group by a.id
	having  count(a.id) > 1;
	