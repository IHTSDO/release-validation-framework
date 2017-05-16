
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-domain-valid-effectivetime

	Assertion:
	EffectiveTime is valid date in MRCM ATTRIBUTE DOMAIN snapshot

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: id=',a.id,' EffectiveTime is not a valid date in MRCM ATTRIBUTE DOMAIN REFSET snapshot file') 	
	from curr_mrcmAttributeDomainRefset_s a	
	where a.effectivetime NOT REGEXP "^[[:digit:]]{8}$";
	commit;
