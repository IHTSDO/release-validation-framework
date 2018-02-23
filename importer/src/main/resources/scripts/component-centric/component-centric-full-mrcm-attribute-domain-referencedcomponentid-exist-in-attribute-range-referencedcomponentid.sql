
/******************************************************************************** 
	component-centric-full-mrcm-attribute-domain-referencedcomponentid-exist-in-attribute-range-referencedcomponentid

	Assertion:
	ReferencedComponentId in MRCM ATTRIBUTE DOMAIN FULL exists in the ReferencedComponentId values of MRCM ATTRIBUTE RANGE FULL

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' ReferencedComponentId in MRCM ATTRIBUTE DOMAIN FULL does not exist in the ReferencedComponentId values of MRCM ATTRIBUTE RANGE FULL') 	
	from curr_mrcmAttributeDomainRefset_f a	
	where a.referencedcomponentid NOT IN (select b.referencedcomponentid from curr_mrcmAttributeRangeRefset_f b);
	commit;
