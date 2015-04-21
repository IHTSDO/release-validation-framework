/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-SNAPSHOT-delta-validation-Inferred-Relationship

  
	Assertion:
	The current data in the Inferred Relationship snapshot file are the same as 
	the data in the current delta file. 




********************************************************************************/

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Relationship: id=',a.id, ' is in delta, but not in snapshot file.') 	
	from curr_relationship_d a
	left join curr_relationship_s b
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
