/*  
 * Active complex maps should not have the same mapTarget in different groups for a given referenced component
 */
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ComplexMap: id=',a.id,': mapTarget=', a.mapTarget, ' is in more than one mapGroup')
 from curr_complexmaprefset_s a
	where a.active = 1
  	and a.mapTarget != ''
	group by a.referencedComponentId, a.mapTarget
	having count(distinct a.mapGroup) > 1;

