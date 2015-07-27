
/*  
	The current data in the mapcorrelationOriginRefset snapshot file are the same as the data in 
	the current delta file. 
*/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('mapcorrelationOriginRefset: id=',a.id, ' is in delta file but not in snapshot file.') 	
	from curr_mapCorrelationOriginRefset_d a
	left join curr_mapCorrelationOriginRefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.refsetid = b.refsetid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.mapTarget = b.mapTarget
		and a.attributeId = b.attributeId
		and a.correlationId = b.correlationId
		and a.contentOriginId = b.contentOriginId
	where 
	b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.refsetid is null
	or b.referencedcomponentid is null
	or b.mapTarget is null
	or b.attributeId is null
	or b.correlationId is null
	or b.contentOriginId is null;
commit;
