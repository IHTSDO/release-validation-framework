
/******************************************************************************** 
	component-centric-snapshot-mrcm-domain-unique-id

	Assertion:
	The current MRCM Domain snapshot file has unique identifiers.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM DOMAIN REFSET: id=',a.id,':Non unique id in current MRCM Domain snapshot file.') 	
	from curr_mrcmDomainRefset_s a
	group by a.id
	having  count(a.id) > 1;
	commit;