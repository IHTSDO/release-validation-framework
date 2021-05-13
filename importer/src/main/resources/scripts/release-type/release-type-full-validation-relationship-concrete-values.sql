
/******************************************************************************** 

	release-type-FULL-validation-Relationship-Concrete-Values

	Assertion:	The current Relationship Concrete Values full file contains all
	previously published data unchanged.


	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.

	select * from curr_relationship_concrete_values_f where id = 'a'
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Relationship Concrete Values: id=',a.id, ' is in prior full file but not in current full file.'),
		a.id,
		'prev_relationship_concrete_values_f'
	from prev_relationship_concrete_values_f a
	left join curr_relationship_concrete_values_f b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.sourceid = b.sourceid
		and a.value = b.value
		and a.relationshipgroup = b.relationshipgroup
		and a.typeid = b.typeid
		and a.characteristictypeid = b.characteristictypeid
		and a.modifierid = b.modifierid	
	where b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.sourceid is null
		or b.value is null
		or b.relationshipgroup is null
		or b.typeid is null
		or b.characteristictypeid is null
		or b.modifierid is null;
	