
/******************************************************************************** 
	component-centric-snapshot-mrcm-module-scope-unique-id.sql

	Assertion:
	The current MRCM Module Scope snapshot file has unique identifiers.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM MODULE SCOPE REFSET: id=',a.id,':Non unique id in current MRCM Module Scope snapshot file.') 	
	from curr_mrcmModuleScopeRefset_s a
	group by a.id
	having  count(a.id) > 1;
	commit;