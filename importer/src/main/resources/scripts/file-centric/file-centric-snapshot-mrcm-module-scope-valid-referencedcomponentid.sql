
/******************************************************************************** 
	file-centric-snapshot-mrcm-module-scope-valid-referencedComponentId

	Assertion:
	ReferencedComponentId value refers to valid concept identifier in MRCM MODULE SCOPE snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedComponentId,
		concat('MRCM MODULE SCOPE: id=',a.id,' : referencedComponentId=',a.referencedComponentId,' MRCM Module Scope Refset contains a ReferencedComponentId that does not exist in the Concept snapshot.') 	
	from curr_mrcmModuleScopeRefset_s a
	left join curr_concept_s b
	on a.referencedComponentId = b.id
	where a.active = 1 and (b.active=0 or b.id is null);
