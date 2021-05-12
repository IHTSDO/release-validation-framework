/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-SNAPSHOT-delta-validation-Relationship-Concrete-Values

  
	Assertion:
	The current data in the Relationship Concrete Values snapshot file are the same as
	the data in the current delta file. 




********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		c.sourceid,
		concat('Relationship Concrete Values: id=',c.id, ' is in delta, but not in snapshot file.'),
		c.id,
		'curr_relationship_concrete_values_d'
	from (select a.* from curr_relationship_concrete_values_d a inner join (select id, MAX(effectivetime) as effectivetime from curr_relationship_concrete_values_d group by id) mostRecent
	on a.id = mostRecent.id and a.effectivetime=mostRecent.effectivetime) c 
	left join curr_relationship_concrete_values_s b
		on c.id = b.id
		and c.effectivetime = b.effectivetime
		and c.active = b.active
		and c.moduleid = b.moduleid
		and c.sourceid = b.sourceid
		and c.value = b.value
		and c.relationshipgroup = b.relationshipgroup
		and c.typeid = b.typeid
		and c.characteristictypeid = b.characteristictypeid
		and c.modifierid = b.modifierid	
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
