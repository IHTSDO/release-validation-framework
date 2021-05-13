/*  
 * There must be actual changes made to previously published complex map refset components in order for them to appear in the current delta.
*/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedComponentId,
		concat('ComplexMap: id=',a.id, ' is in the detla file, but no actual changes made since the previous release.'),
		a.id,
		'curr_complexmaprefset_d'
	from curr_complexmaprefset_d a
	left join prev_complexmaprefset_s b
	on a.id = b.id
	and a.active = b.active
	and a.moduleid = b.moduleid
	and a.refsetid = b.refsetid
	and a.referencedcomponentid = b.referencedcomponentid
	and a.mapGroup = b.mapGroup
	and a.mapPriority = b.mapPriority
	and a.mapRule = b.mapRule
	and a.mapAdvice = b.mapAdvice
	and a.mapTarget = b.mapTarget
	and a.correlationId = b.correlationId
	where b.id is not null;
	