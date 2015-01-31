
/*  
	The current full stated relationship file consists of the previously published full file and the changes for the current release
*/

/* view of current delta, derived from current full */
	drop table if exists v_temp_table;
	create table if not exists v_temp_table like prev_stated_relationship_f;
	
	insert into v_temp_table
	select * from curr_stated_relationship_d;
	commit;
	
	insert into v_temp_table
	select * from prev_stated_relationship_f;
	commit;
/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Stated relationship: id=',a.id, ': Stated relationship is in current full file, but not in prior full file.') 	
	from curr_stated_relationship_f a
	left join v_temp_table b
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
	concat('Stated relationship: id=',a.id, ': Stated relationship is in prior full file, but not in current full file.')
	from v_temp_table a
	left join curr_stated_relationship_f b 
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

 drop table if exists v_temp_table;
