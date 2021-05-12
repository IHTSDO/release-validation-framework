
/******************************************************************************** 
	component-centric-delta-mrcm-attribute-range-referencedcomponentId-exist-in-attribute-domain-refset-referencedcomponentid

	Assertion:
	ReferencedComponentId in MRCM ATTRIBUTE RANGE DELTA exists in the ReferencedComponentId values of MRCM ATTRIBUTE DOMAIN DELTA

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id,' ReferencedComponentId in MRCM ATTRIBUTE RANGE DELTA does not exist in the ReferencedComponentId values of MRCM ATTRIBUTE DOMAIN DELTA'),
		a.id,
		'curr_mrcmattributerangerefset_d'
	from curr_mrcmattributerangerefset_d a
	where a.referencedcomponentid NOT IN (select b.referencedcomponentid from curr_mrcmattributedomainrefset_d b);
	commit;
