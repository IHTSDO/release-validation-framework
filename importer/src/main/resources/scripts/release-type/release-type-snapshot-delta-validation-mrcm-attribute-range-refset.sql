/******************************************************************************** 
	release-type-snapshot-delta-validation-mrcm-attribute-range-refset

	Assertion:
	The current data in the MRCM ATTRIBUTE RANGE REFSET snapshot file are the same as the data in 
	the current delta file. 
********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id, ' is in delta file but not in snapshot file.') 	
	from curr_mrcmAttributeRangeRefset_d a
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