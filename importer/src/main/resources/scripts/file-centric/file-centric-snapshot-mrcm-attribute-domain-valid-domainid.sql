
/******************************************************************************** 
	file-centric-snapshot-mrcm-attribute-domain-valid-domainId

	Assertion:
	DomainId value refers to valid concept identifier in MRCM ATTRIBUTE DOMAIN snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.domainid,
		concat('MRCM ATTRIBUTE DOMAIN: id=',a.id,' : DomainId=',a.domainid,' MRCM Attribute Domain Refset contains a DomainId that does not exist in the Concept snapshot.') 	
	from curr_mrcmAttributeDomainRefset_s a
	left join curr_concept_s b
	on a.domainid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);
