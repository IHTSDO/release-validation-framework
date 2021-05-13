
/******************************************************************************** 
	component-centric-delta-mrcm-attribute-domain-referencedcomponentid-exist-in-attribute-range-referencedcomponentid

	Assertion:
	ReferencedComponentId in MRCM ATTRIBUTE DOMAIN DELTA exists in the ReferencedComponentId values of MRCM ATTRIBUTE RANGE DELTA

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' ReferencedComponentId in MRCM ATTRIBUTE DOMAIN DELTA does not exist in the ReferencedComponentId values of MRCM ATTRIBUTE RANGE DELTA'),
		a.id,
        'curr_mrcmattributedomainrefset_d'
	from curr_mrcmattributedomainrefset_d a
	where a.referencedcomponentid NOT IN (select b.referencedcomponentid from curr_mrcmattributerangerefset_d b);
	commit;
