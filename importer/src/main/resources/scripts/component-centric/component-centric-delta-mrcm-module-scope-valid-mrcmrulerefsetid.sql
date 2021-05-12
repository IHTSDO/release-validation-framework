
/******************************************************************************** 
	component-centric-delta-mrcm-module-scope-valid-mrcmrulerefsetid

	Assertion:
	MrcmRuleRefsetId in MRCM MODULE SCOPE DELTA exists in the RefsetId values of MRCM DOMAIN DELTA or MRCM ATTRIBUTE DOMAIN DELTA or MRCM ATTRIBUTE RANGE DELTA

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.mrcmrulerefsetid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id,' MrcmRuleRefsetId in MRCM MODULE SCOPE DELTA does not exist in the RefsetId values of MRCM DOMAIN DELTA or MRCM ATTRIBUTE DOMAIN DELTA or MRCM ATTRIBUTE RANGE DELTA'),
		a.id,
        'curr_mrcmmodulescoperefset_d'
	from curr_mrcmmodulescoperefset_d a
	where a.mrcmrulerefsetid NOT IN (select b.refsetid from curr_mrcmdomainrefset_d b
									union all select c.refsetid from curr_mrcmattributedomainrefset_d c
									union all select d.refsetid from curr_mrcmattributerangerefset_d d);
	commit;
