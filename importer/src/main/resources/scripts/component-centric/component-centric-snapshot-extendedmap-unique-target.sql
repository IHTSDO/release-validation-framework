/*
 * Active extended maps should not have the same mapTarget within different groups for a given refereced component
 * 
 */
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,': mapTarget=',a.mapTarget,' is in more than one mapGroup')
 from curr_extendedmaprefset_s a
	where a.active = 1
  	and a.mapTarget != ''
	group by a.referencedComponentId, a.mapTarget
	having count(distinct a.mapGroup) > 1;
 commit;
