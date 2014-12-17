/*  
	The current stated relationship delta file is an accurate derivative of the current full file
*/

/* 	view of current delta, derived from current full */
	drop table if exists temp_view;
  create table if not exists temp_view like curr_stated_relationship_f;
  insert into temp_view
	select a.*
	from curr_stated_relationship_f a
	where a.effectivetime = '<CURRENT-RELEASE-DATE>'; 

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Stated relationship: id=',a.id, ': Relationship is in delta file, but not in FULL file.') 	
	from curr_stated_relationship_d a
	left join temp_view b 
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
	or b.sourceid is null
	or b.destinationid is null
	or b.relationshipgroup is null
	or b.typeid is null
	or b.characteristictypeid is null
	or b.modifierid is null;

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Stated relationship: id=',a.id, ': Relationship is in FULL file, but not in delta file.') 
	from temp_view a
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
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.sourceid is null
	or b.destinationid is null
	or b.relationshipgroup is null
	or b.typeid is null
	or b.characteristictypeid is null
	or b.modifierid is null;

commit;
drop table if exists temp_view;


