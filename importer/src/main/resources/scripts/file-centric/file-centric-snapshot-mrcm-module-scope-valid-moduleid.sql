
/******************************************************************************** 
	file-centric-snapshot-mrcm-module-scope-valid-moduleId

	Assertion:
	ModuleId value refers to valid concept identifier in MRCM MODULE SCOPE snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.moduleid,
		concat('MRCM MODULE SCOPE: id=',a.id,' : moduleId=',a.moduleid,' MRCM Module Scope Refset contains a ModuleId that does not exist in the Concept snapshot.') 	
	from curr_mrcmModuleScopeRefset_s a
	left join curr_concept_s b
	on a.moduleid = b.id
	where a.active = 1 and (b.active=0 or b.id is null);
