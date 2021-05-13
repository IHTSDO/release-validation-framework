
/******************************************************************************** 
	file-centric-snapshot-map-correlation-origin-unique-id.sql
	Assertion:
	ID is unique in the map correlation origin refset snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('id=',a.id, ':Non unique id in the MapCorrelationOrigin Snapshot file.') ,
		a.id,
		'curr_mapcorrelationoriginrefset_s'
	from curr_mapcorrelationoriginrefset_s a
	group by a.id
	having  count(a.id) > 1;
	