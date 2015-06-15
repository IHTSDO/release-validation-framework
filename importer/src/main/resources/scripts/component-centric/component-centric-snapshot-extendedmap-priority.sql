/*
 * Active extended maps that are rule based have sequentially ordered mapPriority values without gaps within each mapGroup
 */
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
	 '<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,': Non-sequential mapPriority:', a.mapPriority)   
 from curr_extendedmaprefset_s a
where a.active = 1
  and a.mapPriority > 1
  and not exists
    (select * from curr_extendedmaprefset_s b
     where a.refSetId = b.refSetId
       and a.referencedComponentId = b.referencedComponentId
       and a.mapGroup = b.mapGroup
       and b.mapPriority = a.mapPriority - 1);
 commit;
 