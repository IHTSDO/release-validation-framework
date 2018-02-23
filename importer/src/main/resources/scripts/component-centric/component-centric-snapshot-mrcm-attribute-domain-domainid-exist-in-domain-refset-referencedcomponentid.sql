
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-domainid-exist-in-domain-refset-referencedcomponentid

	Assertion:
	DomainId in MRCM ATTRIBUTE DOMAIN SNAPSHOT exists in the ReferencedComponentId values of MRCM DOMAIN SNAPSHOT

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.domainId,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' DomainId in MRCM ATTRIBUTE DOMAIN SNAPSHOT does not exist in the ReferencedComponentId values of MRCM DOMAIN SNAPSHOT') 	
	from curr_mrcmAttributeDomainRefset_s a	
	where a.domainId NOT IN (select b.referencedcomponentid from curr_mrcmDomainRefset_s b);
	commit;
