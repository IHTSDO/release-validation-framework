
/******************************************************************************** 
	component-centric-snapshot-mrcm-attribute-range-valid-effectivetime

	Assertion:
	EffectiveTime is valid date in MRCM ATTRIBUTE RANGE snapshot

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE RANGE REFSET: id=',a.id,' EffectiveTime is not a valid date in MRCM ATTRIBUTE RANGE REFSET snapshot file') 	
	from curr_mrcmAttributeRangeRefset_s a	
	where a.effectivetime NOT REGEXP "^[[:digit:]]{8}$";
	commit;
