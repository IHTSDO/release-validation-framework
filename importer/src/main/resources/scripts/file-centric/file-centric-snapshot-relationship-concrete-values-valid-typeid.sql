
/******************************************************************************** 
	file-centric-snapshot-relationship-concrete-values-valid-typeid

	Assertion:
	All type ids found in the Relationship Concrete Values snapshot file exist in the Concept snapshot file

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('RELATIONSHIP CONCRETE VALUES: id=',a.id, ': Relationship Concrete Values contains a type id that does not exist in the Concept snapshot file.'),
		a.id,
		'curr_relationship_concrete_values_s'
	from curr_relationship_concrete_values_s a
	left join curr_concept_s b on a.typeid = b.id
	where b.id is null;