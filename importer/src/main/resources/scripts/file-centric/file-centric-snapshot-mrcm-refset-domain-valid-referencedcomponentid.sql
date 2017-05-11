insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM Domain RefSet:',a.referencedcomponentid, ':Invalid referencedComponentId in MRCM Domain Refset snapshot.')
	from curr_mrcmDomainRefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where a.active=0 or b.id is null;