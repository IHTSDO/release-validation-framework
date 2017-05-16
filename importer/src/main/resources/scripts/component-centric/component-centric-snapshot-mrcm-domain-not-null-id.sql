
/******************************************************************************** 
	component-centric-snapshot-validation-mrcm-domain-not-null-id

	Assertion:
	ID is a not null in MRCM DOMAIN REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM DOMAIN REFSET: effectivetime=',a.effectivetime,' : active=',a.active,' : moduleid=',a.moduleid,' : refsetid=',a.refsetid,' : referencedcomponentid',a.referencedcomponentid, ' ID is a null value in MRCM DOMAIN REFSET snapshot file.') 	
	from curr_mrcmDomainRefset_s a	
	where a.id is null;
