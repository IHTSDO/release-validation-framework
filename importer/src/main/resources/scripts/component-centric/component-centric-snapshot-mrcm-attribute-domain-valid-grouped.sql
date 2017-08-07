
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-valid-grouped

	Assertion:
	Grouped value is in (0,1) in MRCM ATTRIBUTE DOMAIN snapshot file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' Grouped value is not in (0,1) in MRCM ATTRIBUTE DOMAIN snapshot file') 	
	from curr_mrcmAttributeDomainRefset_s a	
	where a.grouped NOT IN (0,1);
	commit;
