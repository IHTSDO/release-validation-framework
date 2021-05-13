/**
 * The current ExpressionAssociationRefset full file contains all previously published data unchanged.
 */

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ExpressionAssociationRefset: id=',a.id, ' is in previous full file but not in current full file.'),
		a.id,
		'prev_expressionassociationrefset_f'
	from prev_expressionassociationrefset_f a
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
