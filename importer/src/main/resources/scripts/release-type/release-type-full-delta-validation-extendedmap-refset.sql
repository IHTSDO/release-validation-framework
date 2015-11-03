/*  
*	Current full extended map refset file consists of the previously published full file and the current delta file
*/
insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ExtendedMap: id=',a.id, ' is in current full file, but not in prior full or current delta file.') 	
	from curr_extendedmaprefset_f a
	left join curr_extendedmaprefset_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
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
		and a.mapCategoryId = b.mapCategoryId
	left join prev_extendedmaprefset_f c
		on a.id = c.id
		and a.effectivetime = c.effectivetime
		and a.active = c.active
		and a.moduleid = c.moduleid
		and a.refsetid = c.refsetid
		and a.referencedcomponentid = c.referencedcomponentid
		and a.mapGroup = c.mapGroup
		and a.mapPriority = c.mapPriority
		and a.mapRule = c.mapRule
		and a.mapAdvice = c.mapAdvice
		and a.mapTarget = c.mapTarget
		and a.correlationId = c.correlationId
		and a.mapCategoryId = c.mapCategoryId
	where ( b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.refsetid is null
		or b.referencedcomponentid is null
		or b.mapGroup is null
		or b.mapPriority is null
		or b.mapRule is null
		or b.mapTarget is null
		or b.correlationId is null
		or b.mapCategoryId is null)
	and ( c.id is null
		or c.effectivetime is null
		or c.active is null
		or c.moduleid is null
		or c.refsetid is null
		or c.referencedcomponentid is null
		or c.mapGroup is null
		or c.mapPriority is null
		or c.mapRule is null
		or c.mapTarget is null
		or c.correlationId is null
		or c.mapCategoryId is null);
commit;
