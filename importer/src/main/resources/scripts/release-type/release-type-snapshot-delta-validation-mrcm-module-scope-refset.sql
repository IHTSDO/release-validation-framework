/******************************************************************************** 
	release-type-snapshot-delta-validation-mrcm-module-scope-refset

	Assertion:
	The current data in the MRCM MODULE SCOPE REFSET snapshot file are the same as the data in
	the current delta file. 
********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id, ' is in delta file but not in snapshot file.')
	from curr_mrcmModuleScopeRefset_d a
	left join curr_mrcmModuleScopeRefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid		
		and a.mrcmrulerefsetid = b.mrcmrulerefsetid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null	
	or b.mrcmrulerefsetid is null;