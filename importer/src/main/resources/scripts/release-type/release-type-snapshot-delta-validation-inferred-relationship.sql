/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-SNAPSHOT-delta-validation-Inferred-Relationship

  
	Assertion:
	The current data in the Inferred Relationship snapshot file are the same as 
	the data in the current delta file. 




********************************************************************************/


  drop table if exists snapshot;
  create table if not exists snapshot like curr_relationship_s;
  insert into snapshot
	select *
	from curr_relationship_s
	where cast(effectivetime as datetime) >
	(select max(cast(effectivetime as datetime))
	 from prev_relationship_f);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESCRIPTION: id=',a.id, ': Concept in snapshot file, but not in delta file.') 	
	from snapshot a
	left join curr_relationship_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.sourceid = b.sourceid
		and a.destinationid = b.destinationid
		and a.relationshipgroup = b.relationshipgroup
		and a.typeid = b.typeid
		and a.characteristictypeid = b.characteristictypeid
		and a.modifierid = b.modifierid	
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or a.sourceid is null
	or a.destinationid is null
	or a.relationshipgroup is null
	or a.typeid is null
	or a.characteristictypeid is null
	or a.modifierid is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESCRIPTION: id=',a.id, ': Concept in delta, but not in snapshot file.') 	
	from curr_relationship_d a
	left join snapshot b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.sourceid = b.sourceid
		and a.destinationid = b.destinationid
		and a.relationshipgroup = b.relationshipgroup
		and a.typeid = b.typeid
		and a.characteristictypeid = b.characteristictypeid
		and a.modifierid = b.modifierid	
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or a.sourceid is null
	or a.destinationid is null
	or a.relationshipgroup is null
	or a.typeid is null
	or a.characteristictypeid is null
	or a.modifierid is null;


	drop table if exists snapshot;
