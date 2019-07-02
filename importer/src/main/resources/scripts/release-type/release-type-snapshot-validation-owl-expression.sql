/*  
	The current OWL Expression snapshot file is an accurate derivative of the current full file
*/

/* 	view of current snapshot, derived from current full */
	drop table if exists temp_owlexpressionrefset_v;
  	create table if not exists temp_owlexpressionrefset_v like curr_owlexpressionrefset_f;
  	insert into temp_owlexpressionrefset_v
	select a.*
	from curr_owlexpressionrefset_f a
	where cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime))
		 from curr_owlexpressionrefset_f z
		 where z.id = a.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('OWL Expression: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.')
	from curr_owlexpressionrefset_s a
	left join temp_owlexpressionrefset_v b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.owlexpression = b.owlexpression
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.owlexpression is null;

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('OWL Expression: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.')
	from temp_owlexpressionrefset_v a
	left join curr_owlexpressionrefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.owlexpression = b.owlexpression
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.owlexpression is null;

commit;
drop table if exists temp_owlexpressionrefset_v;


