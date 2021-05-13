/*
 * Active complex map refset members must have a non blank mapTarget for higher map group.
 */
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
	a.referencedComponentId,
 	concat('ComplexMap: id=',a.id,': Higher-level map group with only blank map targets'),
 	a.id,
 	'curr_complexmaprefset_s'
 from curr_complexmaprefset_s a
	where a.active = 1
  	and a.mapGroup > 1
  	and not exists
    (select * from curr_complexmaprefset_s b
     where a.refSetId = b.refSetId
       and a.referencedComponentId = b.referencedComponentId
       and a.mapGroup = b.mapGroup
       and b.mapTarget != '');
commit;