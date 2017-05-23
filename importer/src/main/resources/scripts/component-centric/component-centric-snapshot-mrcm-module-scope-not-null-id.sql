
/******************************************************************************** 
	component-centric-snapshot-validation-mrcm-module-scope-not-null-id

	Assertion:
	ID is a not null in MRCM MODULE SCOPE REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM MODULE SCOPE REFSET: effectivetime=',a.effectivetime,' : active=',a.active,' : moduleid=',a.moduleid,' : refsetid=',a.refsetid,' : referencedcomponentid',a.referencedcomponentid,' : mrcmrulerefsetid=',a.mrcmrulerefsetid, ' ID is a null value in MRCM MODULE SCOPE REFSET snapshot file') 	
	from curr_mrcmModuleScopeRefset_s a	
	where a.id is null;
