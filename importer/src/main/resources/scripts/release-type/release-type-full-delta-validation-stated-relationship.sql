
/*  
	The current full stated relationship file consists of the previously published full file and the changes for the current release
*/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Stated relationship: id=',a.id, ' is in current full file, but not in prior full or current delta file.') 	
	from curr_stated_relationship_f a
	left join curr_stated_relationship_d b
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
	left join prev_stated_relationship_f c
		on a.id = c.id
		and a.effectivetime = c.effectivetime
		and a.active = c.active
		and a.moduleid = c.moduleid
		and a.sourceid = c.sourceid
		and a.destinationid = c.destinationid
		and a.relationshipgroup = c.relationshipgroup
		and a.typeid = c.typeid
		and a.characteristictypeid = c.characteristictypeid
		and a.modifierid = c.modifierid
	where ( b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.sourceid is null
		or b.destinationid is null
		or b.relationshipgroup is null
		or b.typeid is null
		or b.characteristictypeid is null
		or b.modifierid is null)
		and ( c.id is null
		or c.effectivetime is null
		or c.active is null
		or c.moduleid is null
		or c.sourceid is null
		or c.destinationid is null
		or c.relationshipgroup is null
		or c.typeid is null
		or c.characteristictypeid is null
		or c.modifierid is null);
commit;


