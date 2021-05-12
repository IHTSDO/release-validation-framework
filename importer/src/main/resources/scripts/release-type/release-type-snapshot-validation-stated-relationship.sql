/*  
	The current stated relationship snapshot file is an accurate derivative of the current full file
*/

/* 	view of current snapshot, derived from current full */
	drop table if exists temp_stated_relationship_v;
  	create table if not exists temp_stated_relationship_v like curr_stated_relationship_f;
  	insert into temp_stated_relationship_v
	select a.*
	from curr_stated_relationship_f a
	where cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime))
		 from curr_stated_relationship_f z
		 where z.id = a.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Stated relationship: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.'),
		a.id,
		'curr_stated_relationship_s'
	from curr_stated_relationship_s a
	left join temp_stated_relationship_v b
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

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Stated relationship: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.'),
		a.id,
		'curr_stated_relationship_f
	from temp_stated_relationship_v a
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
	or b.sourceid is null
	or b.destinationid is null
	or b.relationshipgroup is null
	or b.typeid is null
	or b.characteristictypeid is null
	or b.modifierid is null;

commit;
drop table if exists temp_stated_relationship_v;


