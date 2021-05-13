/*  
 * Active complex maps should not have the same mapTarget in different groups for a given referenced component
 */
 
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
	'<ASSERTIONUUID>',
 	a.referencedComponentId,
 	concat('ComplexMap: id=',a.id,': mapTarget=', a.mapTarget, ' is in more than one mapGroup'),
 	a.id,
 	'curr_complexmaprefset_s'
 from curr_complexmaprefset_s a
	where a.active = 1
  	and a.mapTarget != ''
	group by a.referencedComponentId, a.mapTarget
	having count(distinct a.mapGroup) > 1;

