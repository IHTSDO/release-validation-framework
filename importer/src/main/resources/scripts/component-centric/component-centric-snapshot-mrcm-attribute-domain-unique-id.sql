
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-unique-id

	Assertion:
	The current MRCM Attribute Domain snapshot file has unique identifiers.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,':Non unique id in current MRCM Attribute Domain snapshot file.') 	
	from curr_mrcmAttributeDomainRefset_s a
	group by a.id
	having  count(a.id) > 1;
	commit;