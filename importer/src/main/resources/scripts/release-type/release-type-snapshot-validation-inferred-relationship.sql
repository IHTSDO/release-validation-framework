/*  
	The current inferred relationship snapshot file is an accurate derivative of the current full file
*/

/* 	view of current snapshot, derived from current full */
	drop table if exists temp_relationship_view;
  	create table if not exists temp_relationship_view like curr_relationship_f;
  	insert into temp_relationship_view
	select a.*
	from curr_relationship_f a
	where cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime))
		 from curr_relationship_f z
		 where a.id = z.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Inferred relationship: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.') 	
	from curr_relationship_s a
	left join temp_relationship_view b
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

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Inferred relationship: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.') 
	from temp_relationship_view a
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
	or b.sourceid is null
	or b.destinationid is null
	or b.relationshipgroup is null
	or b.typeid is null
	or b.characteristictypeid is null
	or b.modifierid is null;

commit;
drop table if exists temp_relationship_view;


