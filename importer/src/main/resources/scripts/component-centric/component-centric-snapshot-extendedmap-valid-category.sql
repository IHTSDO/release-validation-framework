
 /*
 *Active extended map has an mapCategoryId in the supported list
 * 
 */ 
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,': invalid mapCategory=', a.mapCategoryId)
 from curr_extendedmaprefset_s a
 where a.active = 1
  and a.mapCategoryId not in (447637006,447638001,447639009,447640006,447641005,447635003);
 commit;
 
/*
 * Active extended map with mapRule starting with IFA has mapCategortyId=447639009
 *
 */
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
	concat('ExtendedMap: id=',a.id,' : mapRule as IFA rule but with an invalid mapCategory:', a.mapCategoryId)
 from curr_extendedmaprefset_s a
 	where a.active = 1
  	and a.mapRule like 'IFA%'
  	and a.mapCategoryId != 447639009;
 commit;
 