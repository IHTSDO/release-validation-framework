
/******************************************************************************** 
	component-centric-snapshot-validation-mrcm-module-scope-valid-uuid-id

	Assertion:
	ID is a valid UUID in MRCM MODULE SCOPE REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id,' ID is not a valid UUID in MRCM MODULE SCOPE REFSET snapshot file') 	
	from curr_mrcmModuleScopeRefset_s a	
	where a.id is not null and a.id NOT REGEXP "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
	commit;
