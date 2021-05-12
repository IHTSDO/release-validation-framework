/********************************************************************************
	file-centric-snapshot-mrcm-refset-attribute-domain-valid-refsetid.sql
	Assertion:
	Refset ID is valid in MRCM Attribute Domain Refset snapshot.

********************************************************************************/
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.refsetid,
		concat('MRCM ATTRIBUTE DOMAIN: id=',a.id,' : refsetId=',a.refsetid,' MRCM Attribute Domain Refset contains a RefsetId that does not exist in the Concept snapshot.'),
		a.id,
		'curr_mrcmattributedomainrefset_s'
	from curr_mrcmattributedomainrefset_s a
	left join curr_concept_s b
	on a.refsetid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);