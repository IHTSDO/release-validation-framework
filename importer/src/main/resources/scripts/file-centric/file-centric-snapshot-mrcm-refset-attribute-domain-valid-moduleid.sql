insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.moduleid,
		concat('MRCM Attribute Domain RefSet:',a.moduleid, ':Invalid moduleId in MRCM Attribute Domain Refset snapshot.')
	from curr_mrcmAttributeDomainRefset_s a
	left join curr_concept_s b
	on a.moduleid = b.id
	where a.active=0 or b.id is null;