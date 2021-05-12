/*
 * Active extended maps should not have the same mapTarget and mapRule within different groups for a given refereced component
 * 
 */
 
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('ExtendedMap: id=',a.id,': mapTarget=',a.mapTarget,' is in more than one mapGroup'),
 	a.id,
 	'curr_extendedmaprefset_s'
 from curr_extendedmaprefset_s a
	where a.active = 1
  	and a.mapTarget != ''
	group by a.referencedComponentId, a.mapTarget, a.mapRule,a.refsetid
	having count(distinct a.mapGroup) > 1;
 commit;
