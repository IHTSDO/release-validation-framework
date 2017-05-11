/********************************************************************************
	file-centric-snapshot-mrcm-refset-attribute-domain-valid-referencedcomponentid.sql
	Assertion:
	Referenced Component ID is valid in MRCM Attribute Domain Refset snapshot.

********************************************************************************/
insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Attribute Domain RefSet:referencedComponentId=',a.referencedcomponentid, ':referencedComponentId in MRCM Attribute Domain Refset snapshot does not exist in Concept snapshot.')
	from curr_mrcmAttributeDomainRefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);