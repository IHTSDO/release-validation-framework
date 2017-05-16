
/******************************************************************************** 
	component-centric-delta-mrcm-module-scope-valid-mrcmrulerefsetid

	Assertion:
	MrcmRuleRefsetId in MRCM MODULE SCOPE DELTA exists in the RefsetId values of MRCM DOMAIN DELTA or MRCM ATTRIBUTE DOMAIN DELTA or MRCM ATTRIBUTE RANGE DELTA

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.mrcmrulerefsetid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id,' MrcmRuleRefsetId in MRCM MODULE SCOPE DELTA does not exist in the RefsetId values of MRCM DOMAIN DELTA or MRCM ATTRIBUTE DOMAIN DELTA or MRCM ATTRIBUTE RANGE DELTA') 	
	from curr_mrcmModuleScopeRefset_d a	
	where a.mrcmrulerefsetid NOT IN (select b.refsetid from curr_mrcmDomainRefset_d b
									union all select c.refsetid from curr_mrcmAttributeDomainRefset_d c
									union all select d.refsetid from curr_mrcmAttributeRangeRefset_d d);
	commit;
