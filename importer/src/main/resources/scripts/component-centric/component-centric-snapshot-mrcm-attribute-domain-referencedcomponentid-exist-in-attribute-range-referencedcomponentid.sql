
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-referencedcomponentid-exist-in-attribute-range-referencedcomponentid

	Assertion:
	ReferencedComponentId in MRCM ATTRIBUTE DOMAIN SNAPSHOT exists in the ReferencedComponentId values of MRCM ATTRIBUTE RANGE SNAPSHOT

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' ReferencedComponentId in MRCM ATTRIBUTE DOMAIN SNAPSHOT does not exist in the ReferencedComponentId values of MRCM ATTRIBUTE RANGE SNAPSHOT') 	
	from curr_mrcmAttributeDomainRefset_s a	
	where a.referencedcomponentid NOT IN (select b.referencedcomponentid from curr_mrcmAttributeRangeRefset_s b);
	commit;
