
/******************************************************************************** 
	component-centric-snapshot-validation-mrcm-attribute-domain-not-null-id

	Assertion:
	ID is a not null in MRCM ATTRIBUTE DOMAIN REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('MRCM ATTRIBUTE DOMAIN REFSET: effectivetime=',a.effectivetime,' : active=',a.active,' : moduleid=',a.moduleid,' : refsetid=',a.refsetid,' : referencedcomponentid',a.referencedcomponentid,' : domainid=',a.domainid, ' ID is a null value in MRCM ATTRIBUTE DOMAIN REFSET snapshot file') 	
	from curr_mrcmAttributeDomainRefset_s a	
	where a.id is null;
