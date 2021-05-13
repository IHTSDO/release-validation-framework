
/******************************************************************************** 
	component-centric-snapshot-concept-inactive-definitionstatusid

	Assertion:
	All inactive concepts have definition status of PRIMITIVE.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		id,
		concat('CONCEPT: id=',id, ':There is an inactive concept whose definition status is not Primitive.'),
		id,
        'curr_concept_s'
	from curr_concept_s 
	where active = '0'
	and definitionstatusid != '900000000000074008';
	