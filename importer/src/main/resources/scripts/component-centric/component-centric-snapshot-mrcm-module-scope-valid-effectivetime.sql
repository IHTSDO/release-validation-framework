
/******************************************************************************** 
	component-centric-snapshot-mrcm-module-scope-valid-effectivetime

	Assertion:
	EffectiveTime is valid date in MRCM MODULE SCOPE snapshot

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id,' EffectiveTime is not a valid date in MRCM MODULE SCOPE REFSET snapshot file') 	
	from curr_mrcmModuleScopeRefset_s a	
	where a.effectivetime NOT REGEXP "^[[:digit:]]{8}$";
	commit;
