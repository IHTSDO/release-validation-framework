
/*  
	The current full inferred relationship file consists of the previously published full file and the changes for the current release
*/

drop temporary table if exists temp_table;

/* view of current delta, derived from current full */
	create table if not exists temp_table (
    id varchar(18),
    effectivetime char(8),
    active char(1),
    moduleid varchar(18),
    sourceid varchar(18),
    destinationid varchar(18),
    relationshipgroup varchar(18),
    typeid varchar(18),
    characteristictypeid varchar(18),
    modifierid varchar(18)
    );

	create  index id_id on temp_table(id);
	create  index id_effectivetime on temp_table(effectivetime);
	create  index id_active on temp_table(active);
	create  index id_moduleid on temp_table(moduleid);
	create  index id_sourceid on temp_table(sourceid);
	create  index id_destinationid on temp_table(destinationid);
	create  index id_relationshipgroup on temp_table(relationshipgroup);
	create  index id_typeid on temp_table(typeid);
	create  index id_characteristictypeid on temp_table(characteristictypeid);
	create  index id_modifierid on temp_table(modifierid);

	
	
	insert into temp_table select * from curr_relationship_d;
	insert into temp_table select *	from prev_relationship_f;

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Inferred relationship: id=',a.id, ': Inferred relationship is in current full file, but not in prior full file.') 	
	from curr_relationship_f a
	left join temp_table b 
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
	concat('Inferred relationship: id=',a.id, ': Inferred relationship is in prior full file, but not in current full file.')
	from temp_table a
	left join curr_relationship_f b 
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
drop table temp_table;
