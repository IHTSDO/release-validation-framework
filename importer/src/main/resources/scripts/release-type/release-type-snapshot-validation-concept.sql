
/*  
	The current concept snapshot file is an accurate derivative of the current full file
*/

/* view of current snapshot, derived from current full */
	drop table if exists temp_concept_snapshot;
  	create table if not exists temp_concept_snapshot like curr_concept_f;
  	insert into temp_concept_snapshot
	select a.*
	from curr_concept_f a
	where cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime))
		 from curr_concept_f z
		 where z.id = a.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.'),
		a.id,
		'curr_concept_s'
	from curr_concept_s a
	left join temp_concept_snapshot b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
	and a.moduleid = b.moduleid
	and a.definitionstatusid = b.definitionstatusid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.definitionstatusid is null;

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.'),
		a.id,
		'curr_concept_f'
	from temp_concept_snapshot a
	left join curr_concept_s b 
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.definitionstatusid = b.definitionstatusid
	where b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.definitionstatusid is null;
	
	commit;
	drop table if exists temp_concept_snapshot;






