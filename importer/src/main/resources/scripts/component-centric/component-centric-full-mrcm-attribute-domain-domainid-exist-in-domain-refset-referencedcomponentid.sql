
/******************************************************************************** 
	component-centric-full-mrcm-attribute-domain-domainid-exist-in-domain-refset-referencedcomponentid

	Assertion:
	DomainId in MRCM ATTRIBUTE DOMAIN FULL exists in the ReferencedComponentId values of MRCM DOMAIN FULL

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.domainId,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' DomainId in MRCM ATTRIBUTE DOMAIN FULL does not exist in the ReferencedComponentId values of MRCM DOMAIN FULL') 	
	from curr_mrcmAttributeDomainRefset_f a	
	where a.domainId NOT IN (select b.referencedcomponentid from curr_mrcmDomainRefset_f b);
	commit;
