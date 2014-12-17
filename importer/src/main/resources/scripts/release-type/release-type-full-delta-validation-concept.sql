
/*  
	The current full concept file consists of the previously published full file aalbnd the changes for the current release
*/

drop table if exists temp_table;

/* view of current delta, derived from current full */
	create table if not exists temp_table like prev_concept_f;
	insert into temp_table select * from curr_concept_d;
	insert into temp_table select *	from prev_concept_f;

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept is in current full file, but not in prior full file.') 	
	from curr_concept_f a
	left join temp_table b 
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

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept is in prior full file, but not in current full file.')
	from temp_table a
	left join curr_concept_f b 
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
drop table temp_table;
