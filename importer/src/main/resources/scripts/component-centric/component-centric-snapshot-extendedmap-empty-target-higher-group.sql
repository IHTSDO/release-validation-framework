 /*
 * Active extended map refset members must have a blank mapTarget or a code that 
 * is valid for the target terminology (and version)
 * 
 */ 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
	 '<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,' : Higher-level map group with only blank map targets') 
 from curr_extendedmaprefset_s a
	where a.active = 1
  	and a.mapGroup > 1
  	and not exists
    (select * from curr_extendedmaprefset_s b
     where a.refSetId = b.refSetId
       and a.referencedComponentId = b.referencedComponentId
       and a.mapGroup = b.mapGroup
       and b.mapTarget != '');
  commit;
