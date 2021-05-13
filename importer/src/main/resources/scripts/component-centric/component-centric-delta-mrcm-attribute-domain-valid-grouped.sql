
/******************************************************************************** 
	component-centric-delta-mrcm-attribute-domain-valid-grouped

	Assertion:
	Grouped value is in (0,1) in MRCM ATTRIBUTE DOMAIN delta file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' Grouped value is not in (0,1) in MRCM ATTRIBUTE DOMAIN delta file'),
		a.id,
        'curr_mrcmattributedomainrefset_d'
	from curr_mrcmattributedomainrefset_d a
	where a.grouped NOT IN (0,1);
	commit;
