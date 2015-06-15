/*  
*	Content in the the current extended map delta file must be in the current full file
*/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ExtendedMap: id=',a.id, ' is in DELTA file, but not in FULL file.') 	
	from curr_extendedmaprefset_d a
	left join curr_extendedmaprefset_f b
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
	where b.id is null
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
	or b.mapCategoryId is null;