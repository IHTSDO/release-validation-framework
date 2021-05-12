
/******************************************************************************** 
	component-centric-snapshot-vtm-active

	Assertion:
	Active VTM simple refset members refer to active components.


********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Simple refset: id=',a.id, ' is an active VTM but refers to an inactive component.'),
		a.id,
        'curr_simplerefset_s'
	from curr_simplerefset_s a
	inner join curr_concept_s b 
	on a.referencedcomponentid = b.id
	where a.active = '1'
	and a.refsetid = '447565001'
	and b.active = '0';