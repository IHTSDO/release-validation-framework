
/******************************************************************************** 
	file-centric-snapshot-map-correlation-origin-referencecomponentid.sql

	Assertion:
	ReferenceComponentIds refers to valid concepts in the Map Correlation Origin  Refset snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MapCorrelationOrigin: referencedcomponentid=',a.referencedcomponentid, 'is an invalid concept') 	
	from curr_mapCorrelationOriginRefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where b.id is null;
