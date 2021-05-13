
/******************************************************************************** 
	component-centric-snapshot-mrcm-module-scope-valid-mrcmrulerefsetid

	Assertion:
	MrcmRuleRefsetId in MRCM MODULE SCOPE SNAPSHOT exists in the RefsetId values of MRCM DOMAIN SNAPSHOT or MRCM ATTRIBUTE DOMAIN SNAPSHOT or MRCM ATTRIBUTE RANGE SNAPSHOT

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.mrcmrulerefsetid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id,' MrcmRuleRefsetId in MRCM MODULE SCOPE SNAPSHOT does not exist in the RefsetId values of MRCM DOMAIN SNAPSHOT or MRCM ATTRIBUTE DOMAIN SNAPSHOT or MRCM ATTRIBUTE RANGE SNAPSHOT'),
		a.id,
        'curr_mrcmmodulescoperefset_s'
	from curr_mrcmmodulescoperefset_s a
	where a.mrcmrulerefsetid NOT IN (select b.refsetid from curr_mrcmdomainrefset_s b
									union all select c.refsetid from curr_mrcmattributedomainrefset_s c
									union all select d.refsetid from curr_mrcmattributerangerefset_s d);
	commit;
