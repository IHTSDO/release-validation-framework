
/******************************************************************************** 
	file-centric-snapshot-map-correlation-origin-referencecomponentid.sql

	Assertion:
	ReferenceComponentIds refers to valid concepts in the Map Correlation Origin  Refset snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		result.referencedcomponentid,
		concat('ReferencedcomponentId=',result.referencedcomponentid, ' in MapCorrelationOriginRefset snapshot is not a concept Id'),
		null,
		'curr_mapcorrelationoriginrefset_s'
	from ( select distinct a.referencedcomponentid
		from curr_mapcorrelationoriginrefset_s a
		left join curr_concept_s b
		on a.referencedcomponentid = b.id
		where b.id is null) as result;
