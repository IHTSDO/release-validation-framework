/*  
*	Changes from the current expressionassociationrefset delta file must be in the current full file
*/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ExpressionAssociation: id=',a.id, ' is in DELTA file, but not in FULL file.'),
		a.id,
		'curr_expressionassociationrefset_d'
	from curr_expressionassociationrefset_d a
	left join curr_expressionassociationrefset_f b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
	and a.moduleid = b.moduleid
	and a.refsetid = b.refsetid
	and a.referencedcomponentid = b.referencedcomponentid
	and a.mapTarget = b.mapTarget
	and a.expression = b.expression
	and a.definitionStatusId = b.definitionStatusId
	and a.correlationId = b.correlationId
	and a.contentOriginId = b.contentOriginId
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.mapTarget is null
	or b.expression is null
	or b.definitionStatusId is null
	or b.correlationId is null
	or b.contentOriginId is null;