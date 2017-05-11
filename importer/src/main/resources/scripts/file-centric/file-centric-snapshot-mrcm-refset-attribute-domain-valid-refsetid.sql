insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.refsetid,
		concat('MRCM Attribute Domain RefSet:',a.refsetid, ':Invalid refsetId in MRCM Attribute Domain Refset snapshot.')
	from curr_mrcmAttributeDomainRefset_s a
	left join curr_concept_s b
	on a.refsetid = b.id
	where a.active=0 or b.id is null;