
/******************************************************************************** 

	release-type-full-validation-mrcm-attribute-range-refset 

	Assertion:	The current MRCM Attribute Range Refset full file contains all 
	previously published data unchanged.


	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.	
********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id, ' is in prior full file but not in current full file.') 	
	from prev_mrcmAttributeRangeRefset_f a
	left join curr_mrcmAttributeRangeRefset_f b
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
	