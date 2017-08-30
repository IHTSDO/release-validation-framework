
/******************************************************************************** 
	file-centric-snapshot-mrcm-module-scope-valid-mrcmrulerefsetid

	Assertion:
	MrcmRuleRefsetId value refers to valid concept identifier in MRCM MODULE SCOPE snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.mrcmrulerefsetid,
		concat('MRCM MODULE SCOPE: id=',a.id,' : mrcmRuleRefsetId=',a.mrcmrulerefsetid,' MRCM Module Scope Refset contains a MrcmRuleRefsetId that does not exist in the Concept snapshot.') 	
	from curr_mrcmModuleScopeRefset_s a
	left join curr_concept_s b
	on a.mrcmrulerefsetid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);
