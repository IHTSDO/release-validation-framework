/********************************************************************************
	file-centric-snapshot-mrcm-refset-domain-valid-moduleid.sql
	Assertion:
	Module ID is valid in MRCM Domain Refset snapshot.

********************************************************************************/
insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		a.moduleid,
		concat('MRCM Domain RefSet:moduleId=',a.moduleid, ':moduleId in MRCM Domain Refset snapshot does not exist in Concept snapshot.')
	from curr_mrcmDomainRefset_s a
	left join curr_concept_s b
	on a.moduleid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);