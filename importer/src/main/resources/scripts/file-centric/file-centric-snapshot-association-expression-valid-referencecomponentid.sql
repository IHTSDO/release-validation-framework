
/******************************************************************************** 
	file-centric-snapshot-association-expression-valid-referencecomponentid.sql

	Assertion:
	ReferenceComponentIds refers to valid concepts in the ASSOCIATION Expression REFSET snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Association expression: referencedcomponentid=',a.referencedcomponentid, 'is an invalid concept') 	
	from curr_expressionAssociationRefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where b.id is null;
