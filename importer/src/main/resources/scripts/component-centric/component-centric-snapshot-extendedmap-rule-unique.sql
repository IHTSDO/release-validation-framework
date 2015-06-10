/*
 * Active extended maps that are rule based do not have multiple entries with the same mapRule and mapTarget in different groups
 */ 
 insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,' : duplicate mapRule and mapTarget in different groups')     
 	from curr_extendedmaprefset_s a, curr_extendedmaprefset_s b
where a.active = 1 and b.active = 1
  and a.refSetId = b.refSetId
  and a.referencedComponentId = b.referencedComponentId
  and a.mapRule = b.mapRule
  and a.mapTarget != ''
  and a.mapTarget = b.mapTarget
  and a.mapGroup < b.mapGroup;
 commit;
 