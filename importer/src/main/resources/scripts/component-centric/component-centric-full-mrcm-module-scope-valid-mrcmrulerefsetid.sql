
/******************************************************************************** 
	component-centric-full-mrcm-module-scope-valid-mrcmrulerefsetid

	Assertion:
	MrcmRuleRefsetId in MRCM MODULE SCOPE FULL exists in the RefsetId values of MRCM DOMAIN FULL or MRCM ATTRIBUTE DOMAIN FULL or MRCM ATTRIBUTE RANGE FULL

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.mrcmrulerefsetid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id,' MrcmRuleRefsetId in MRCM MODULE SCOPE FULL does not exist in the RefsetId values of MRCM DOMAIN FULL or MRCM ATTRIBUTE DOMAIN FULL or MRCM ATTRIBUTE RANGE FULL') 	
	from curr_mrcmModuleScopeRefset_f a	
	where a.mrcmrulerefsetid NOT IN (select b.refsetid from curr_mrcmDomainRefset_f b
									union all select c.refsetid from curr_mrcmAttributeDomainRefset_f c
									union all select d.refsetid from curr_mrcmAttributeRangeRefset_f d);
	commit;
