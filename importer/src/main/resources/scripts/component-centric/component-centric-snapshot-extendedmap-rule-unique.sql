/*
 * Active extended maps that are rule based do not have multiple entries with the same mapRule and mapTarget in different groups
 */ 
 insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id,' : duplicate mapRule and mapTarget in different groups'),
 	a.id,
    'curr_extendedmaprefset_s'
 	from curr_extendedmaprefset_s a, curr_extendedmaprefset_s b
where a.active = 1 and b.active = 1
  and a.refSetId = b.refSetId
  and a.referencedComponentId = b.referencedComponentId
  and a.mapRule = b.mapRule
  and a.mapTarget != ''
  and a.mapTarget = b.mapTarget
  and a.mapGroup < b.mapGroup;
 commit;
 