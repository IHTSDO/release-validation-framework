
/*  
	The current full ExpressionAssociationRefset file consists of the previously published full file and the changes for the current release
*/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.referencedcomponentid,
	concat('ExpressionAssociationRefset: id=',a.id, ' is in current full file, but not in prior full or current delta file.'),
	a.id,
	'curr_expressionassociationrefset_f'
	from curr_expressionassociationrefset_f a
	left join curr_expressionassociationrefset_d b
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
	left join prev_expressionassociationrefset_f c
		on a.id = c.id
		and a.effectivetime = c.effectivetime
		and a.active = c.active
		and a.moduleid = c.moduleid
		and a.refsetid = c.refsetid
		and a.referencedcomponentid = c.referencedcomponentid
		and a.mapTarget = c.mapTarget
		and a.expression = c.expression
		and a.definitionStatusId = c.definitionStatusId
		and a.correlationId = c.correlationId
		and a.contentOriginId = c.contentOriginId
	where ( b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.mapTarget is null
	or b.expression is null
	or b.definitionStatusId is null
	or b.correlationId is null
	or b.contentOriginId is null)
	and ( c.id is null
	or c.effectivetime is null
	or c.active is null
	or c.moduleid is null
	or c.refsetid is null
	or c.referencedcomponentid is null
	or c.mapTarget is null
	or c.expression is null
	or c.definitionStatusId is null
	or c.correlationId is null
	or c.contentOriginId is null);
commit;
