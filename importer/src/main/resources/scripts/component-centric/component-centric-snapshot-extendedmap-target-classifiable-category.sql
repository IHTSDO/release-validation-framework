/*
 * MapTarget should not be empty for classifiable mapCategory
 * Active extended map does not have empty mapTarget when mapCategoryId=447637006,447639009 (propery classified or context dependent)
 *
 */

insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id,' : Empty mapTarget for classifiable mapCategory'),
 	a.id,
    'curr_extendedmaprefset_s'
 from curr_extendedmaprefset_s a
 where a.active = 1
  	and a.mapTarget = ''
  	and a.mapCategoryId in (447637006,447639009);
 commit;
 
 
  
/*
 * Active extended map has an empty mapTarget when mapCategoryId!=447637006,447639009 (propery classified or context dependent)
 */
 
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,concat('ExtendedMap: id=',a.id,' : Non-empty mapTarget for unclassifiable mapCategory'),
 	a.id,
    'curr_extendedmaprefset_s'
 from curr_extendedmaprefset_s a
 where a.active = 1
  	and a.mapTarget != ''
  	and a.mapCategoryId not in (447637006,447639009);
 commit;
 