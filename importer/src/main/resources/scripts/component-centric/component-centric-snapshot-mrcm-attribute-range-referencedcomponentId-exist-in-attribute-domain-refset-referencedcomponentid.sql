
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-range-referencedcomponentId-exist-in-attribute-domain-refset-referencedcomponentid

	Assertion:
	ReferencedComponentId in MRCM ATTRIBUTE RANGE SNAPSHOT exists in the ReferencedComponentId values of MRCM ATTRIBUTE DOMAIN SNAPSHOT

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id,' ReferencedComponentId in MRCM ATTRIBUTE RANGE SNAPSHOT does not exist in the ReferencedComponentId values of MRCM ATTRIBUTE DOMAIN SNAPSHOT') 	
	from curr_mrcmAttributeRangeRefset_s a	
	where a.referencedcomponentid NOT IN (select b.referencedcomponentid from curr_mrcmAttributeDomainRefset_s b);
	commit;
