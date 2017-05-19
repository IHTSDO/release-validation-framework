/********************************************************************************
	file-centric-snapshot-mrcm-refset-domain-valid-referencedcomponentid.sql
	Assertion:
	Referenced Component ID is valid in MRCM Domain Refset snapshot.

********************************************************************************/
insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM DOMAIN: id=',a.id,' : referencedComponentId=',a.referencedcomponentid,' MRCM Domain Refset contains a ReferencedComponentId that does not exist in the Concept snapshot.')
	from curr_mrcmDomainRefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);