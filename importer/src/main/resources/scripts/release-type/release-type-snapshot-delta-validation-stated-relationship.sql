/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-SNAPSHOT-delta-validation-Stated-Relationship

  
	Assertion:
	The current data in the Stated Relationship snapshot file are the same as the 
	data in the current delta file. 

	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.


********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('StatedRelationship: id=',a.id, ' is in delta but not in snapshot file.') 	
	from curr_stated_relationship_d a
	left join curr_stated_relationship_s b
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