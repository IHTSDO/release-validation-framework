
/******************************************************************************** 
	component-centric-snapshot-stated-inactive-modifier
	
	Assertion:
	Stated relationship modifier is always Some.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('RELATIONSHIP: id=',a.id, ': Stated relationship has a non -SOME- modifier.'),
        a.id,
        'curr_stated_relationship_s'
	from curr_stated_relationship_s a
	where a.active = '1'
	and a.modifierid != '900000000000451002';
	