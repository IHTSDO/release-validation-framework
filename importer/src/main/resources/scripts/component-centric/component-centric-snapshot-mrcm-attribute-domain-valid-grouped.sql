
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-valid-grouped

	Assertion:
	Grouped value is either 0 or 1 in MRCM ATTRIBUTE DOMAIN snapshot file

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' Grouped value is invalid ', a.grouped) 	
	from curr_mrcmAttributeDomainRefset_s a	
	where a.grouped != '0' or a.grouped != '1';
	commit;
