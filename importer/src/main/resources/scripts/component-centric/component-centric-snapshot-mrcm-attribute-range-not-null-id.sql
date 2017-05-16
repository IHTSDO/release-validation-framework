
/******************************************************************************** 
	component-centric-snapshot-validation-mrcm-attribute-range-not-null-id

	Assertion:
	ID is a not null in MRCM ATTRIBUTE RANGE REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: effectivetime=',a.effectivetime,' : active=',a.active,' : moduleid=',a.moduleid,' : refsetid=',a.refsetid,' : referencedcomponentid',a.referencedcomponentid,' : rulestrengthid=',a.rulestrengthid,' :contenttypeid=',a.contenttypeid, ' ID is a null value in MRCM ATTRIBUTE RANGE REFSET snapshot file') 	
	from curr_mrcmAttributeRangeRefset_s a	
	where a.id is null;
