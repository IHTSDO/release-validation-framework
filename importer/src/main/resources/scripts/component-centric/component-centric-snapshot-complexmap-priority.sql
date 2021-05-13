/*
 * Active complex maps that are rule based have sequentially ordered mapPriority values without gaps within each mapGroup
 */
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
	 '<ASSERTIONUUID>',
 	a.referencedComponentId,
 	concat('ComplexMap: id=',a.id,': Non-sequential mapPriority:', a.mapPriority),
 	a.id,
    'curr_complexmaprefset_s'
 from curr_complexmaprefset_s a
where a.active = 1
  and a.mapPriority > 1
  and not exists
    (select * from curr_complexmaprefset_s b
     where a.refSetId = b.refSetId
       and a.referencedComponentId = b.referencedComponentId
       and a.mapGroup = b.mapGroup
       and b.mapPriority = a.mapPriority - 1);
 commit;
 