
/******************************************************************************** 
	component-centric-snapshot-mrcm-domain-valid-effectivetime

	Assertion:
	EffectiveTime is valid date in MRCM DOMAIN snapshot

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM DOMAIN REFSET: id=',a.id,' EffectiveTime is not a valid date in MRCM DOMAIN REFSET snapshot file') 	
	from curr_mrcmDomainRefset_s a	
	where a.effectivetime NOT REGEXP "^[[:digit:]]{8}$";
	commit;
