/*  
	The current MRCM Attribute Range Refset snapshot file is an accurate derivative of the current full file
*/

/* 	view of current snapshot, derived from current full */
	drop table if exists temp_mrcmattributerangerefset_v;
  	create table if not exists temp_mrcmattributerangerefset_v like curr_mrcmAttributeRangeRefset_f;
  	insert into temp_mrcmattributerangerefset_v
	select a.*
	from curr_mrcmAttributeRangeRefset_f a
	where cast(a.effectivetime as datetime) =
		(select max(cast(z.effectivetime as datetime))
		 from curr_mrcmAttributeRangeRefset_f z
		 where z.id = a.id);

/* in the snapshot; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Attribute Range Refset: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.')
	from curr_mrcmAttributeRangeRefset_s a
	left join temp_mrcmattributerangerefset_v b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.rangeconstraint = b.rangeconstraint
		and a.attributerule = b.attributerule
		and a.rulestrengthid = b.rulestrengthid
		and a.contenttypeid = b.contenttypeid
	where b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.refsetid is null
		or b.referencedcomponentid is null
		or b.rangeconstraint is null
		or b.attributerule is null
		or b.rulestrengthid is null
		or b.contenttypeid is null;

/* in the full; not in the snapshot */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Attribute Range Refset: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.')
	from temp_mrcmattributerangerefset_v a
	left join curr_mrcmAttributeRangeRefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.rangeconstraint = b.rangeconstraint
		and a.attributerule = b.attributerule
		and a.rulestrengthid = b.rulestrengthid
		and a.contenttypeid = b.contenttypeid
	where b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.refsetid is null
		or b.referencedcomponentid is null
		or b.rangeconstraint is null
		or b.attributerule is null
		or b.rulestrengthid is null
		or b.contenttypeid is null;

commit;
drop table if exists temp_mrcmattributerangerefset_v;