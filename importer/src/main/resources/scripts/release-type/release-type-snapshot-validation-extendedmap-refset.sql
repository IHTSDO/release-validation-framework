/*  
*	Content in the the current extended map snapshot file must be in the current full file
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ExtendedMap: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.') 	
	from curr_extendedmaprefset_s a
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
commit;


/*  
*	Snapshot view derived from the current full file must be in in the current snapshot file
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ExtendedMap: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.') 	
	from curr_extendedmaprefset_f a
	left join curr_extendedmaprefset_s b
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
	where
	 cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime))
		 from curr_extendedmaprefset_f z
		 where z.id = a.id)
	and
	( b.id is null
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
	or b.mapCategoryId is null);
commit;
