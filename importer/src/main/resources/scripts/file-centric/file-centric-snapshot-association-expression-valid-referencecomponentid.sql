
/******************************************************************************** 
	file-centric-snapshot-association-expression-valid-referencecomponentid.sql

	Assertion:
	ReferenceComponentIds refers to valid concepts in the ASSOCIATION Expression REFSET snapshot file.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('Referencedcomponentid=',a.referencedcomponentid, '  in AssociationExpressionis snapshot not a concept id.'),
		a.id,
        'curr_expressionassociationrefset_s'
	from curr_expressionassociationrefset_s a
    	left join curr_concept_s b
    	on a.referencedcomponentid = b.id
    	where b.id is null;
