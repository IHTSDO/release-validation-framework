
/******************************************************************************** 
	component-centric-delta-mrcm-attribute-domain-domainid-exist-in-domain-refset-referencedcomponentid

	Assertion:
	DomainId in MRCM ATTRIBUTE DOMAIN DELTA exists in the ReferencedComponentId values of MRCM DOMAIN DELTA

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.domainId,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' DomainId in MRCM ATTRIBUTE DOMAIN DELTA does not exist in the ReferencedComponentId values of MRCM DOMAIN DELTA') 	
	from curr_mrcmAttributeDomainRefset_d a	
	where a.domainId NOT IN (select b.referencedcomponentid from curr_mrcmDomainRefset_d b);
	commit;
