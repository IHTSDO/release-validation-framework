/*
 * Active extended maps that are rule based have at least one mapGroup
 */
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
	 concat('ExtendedMap: id=',a.id,' : mapgroup is blank for rule based map' ),
	 a.id,
     'curr_extendedmaprefset_s'
 from curr_extendedmaprefset_s a
	where a.active = 1 
  	and (a.mapGroup is null
  	or a.mapGroup ='')
  	and a.refSetId in (select distinct(b.refSetId) from curr_extendedmaprefset_s b
     where b.active = 1 and mapRule != '');
 commit;
 
 
 /*
 * Active extended maps that are rule based have sequentially ordered mapGroup values without gaps
 *
 */
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id,' : Non-sequential map group:',a.mapGroup),
 	a.id,
    'curr_extendedmaprefset_s'
 from curr_extendedmaprefset_s a
	where a.active = 1
  	and a.mapGroup > 1
  	and not exists
    	(select * from curr_extendedmaprefset_s b
     	where a.refSetId = b.refSetId
       	and a.referencedComponentId = b.referencedComponentId
       	and b.mapGroup = a.mapGroup - 1);
 commit;