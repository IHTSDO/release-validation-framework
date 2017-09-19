
/******************************************************************************** 
	file-centric-snapshot-mrcm-module-scope-valid-refsetId

	Assertion:
	RefsetId value refers to valid concept identifier in MRCM MODULE SCOPE snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.refsetId,
		concat('MRCM MODULE SCOPE: id=',a.id,' : refsetId=',a.refsetId,' MRCM Module Scope Refset contains a RefsetId that does not exist in the Concept snapshot.') 	
	from curr_mrcmModuleScopeRefset_s a
	left join curr_concept_s b
	on a.refsetId = b.id
	where a.active = 1 and (b.active=0 or b.id is null);
