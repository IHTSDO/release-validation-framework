
/******************************************************************************** 
	component-centric-snapshot-ctv3-active

	Assertion:
	CTV3 simple map refset members are always active.

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MEMBER: id=',a.id, ': CTV3 simple map refset member is not active.'),
		a.id,
		'curr_simplemaprefset_s'
	from curr_simplemaprefset_s a
	where a.refsetid = '900000000000497000'
	and a.active != '1';