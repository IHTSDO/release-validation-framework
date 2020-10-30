
/******************************************************************************** 
	file-centric-snapshot-relationship-concrete-values-valid-sourceid

	Assertion:
	All source ids found in the Relationship Concrete Values snapshot file exist in the Concept snapshot file

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceId,
		concat('RELATIONSHIP CONCRETE VALUES: id=',a.id, ': Relationship Concrete Values contains a source id that does not exist in the Concept snapshot file.')
	from curr_relationship_concrete_values_s a
	left join curr_concept_s b on a.sourceid = b.id
	where b.id is null;