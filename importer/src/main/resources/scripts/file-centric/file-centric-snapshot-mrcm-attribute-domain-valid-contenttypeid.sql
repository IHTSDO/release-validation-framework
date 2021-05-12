
/******************************************************************************** 
	file-centric-snapshot-mrcm-attribute-domain-valid-contenttypeid

	Assertion:
	ContentTypeId value refers to valid concept identifier in MRCM ATTRIBUTE DOMAIN snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.contenttypeid,
		concat('MRCM ATTRIBUTE DOMAIN: id=',a.id,' : contentTypeId=',a.contenttypeid,' MRCM Attribute Domain Refset contains a ContentTypeId that does not exist in the Concept snapshot.'),
		a.id,
		'curr_mrcmattributedomainrefset_s'
	from curr_mrcmattributedomainrefset_s a
	left join curr_concept_s b
	on a.contenttypeid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);
