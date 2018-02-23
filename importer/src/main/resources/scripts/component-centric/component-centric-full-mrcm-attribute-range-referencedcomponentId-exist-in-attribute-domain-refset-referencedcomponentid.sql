
/******************************************************************************** 
	component-centric-full-mrcm-attribute-range-referencedcomponentId-exist-in-attribute-domain-refset-referencedcomponentid

	Assertion:
	ReferencedComponentId in MRCM ATTRIBUTE RANGE FULL exists in the ReferencedComponentId values of MRCM ATTRIBUTE DOMAIN FULL

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id,' ReferencedComponentId in MRCM ATTRIBUTE RANGE FULL does not exist in the ReferencedComponentId values of MRCM ATTRIBUTE DOMAIN FULL') 	
	from curr_mrcmAttributeRangeRefset_f a	
	where a.referencedcomponentid NOT IN (select b.referencedcomponentid from curr_mrcmAttributeDomainRefset_f b);
	commit;
