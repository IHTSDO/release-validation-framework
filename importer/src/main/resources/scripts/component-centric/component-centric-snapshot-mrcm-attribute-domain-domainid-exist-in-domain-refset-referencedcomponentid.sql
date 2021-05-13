
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-domainid-exist-in-domain-refset-referencedcomponentid

	Assertion:
	DomainId in MRCM ATTRIBUTE DOMAIN SNAPSHOT exists in the ReferencedComponentId values of MRCM DOMAIN SNAPSHOT

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.domainId,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' DomainId in MRCM ATTRIBUTE DOMAIN SNAPSHOT does not exist in the ReferencedComponentId values of MRCM DOMAIN SNAPSHOT'),
		a.id,
		'curr_mrcmattributedomainrefset_s'
	from curr_mrcmattributedomainrefset_s a
	where a.domainId NOT IN (select b.referencedcomponentid from curr_mrcmdomainrefset_s b);
	commit;
