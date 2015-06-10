  
/*
 * Active extended maps must have valid  mapAdvice values for corresponding mapCategory
 *
 * 1. Active extended maps do not have duplicated values in mapAdvice. This is part of the parser described above and not easily implemented as an SQL check.
 * 2. Active extended maps have mapAdvice values that are sorted. Again, part of parser desribed above and not easily implemented as an SQL check
 
 */
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 <RUNID>,
 '<ASSERTIONUUID>',
 '<ASSERTIONTEXT>',
 concat('ExtendedMap: id=',a.id,' : misaligned mapCategory and mapAdvice for ambiguous WHO concepts')     
 from curr_extendedmaprefset_s a
where a.active = 1
  and (
   (a.mapCategoryId = 447635003 AND a.mapAdvice not like '%MAPPING GUIDANCE FROM WHO IS AMBIGUOUS%') or
   (a.mapCategoryId != 447635003 AND a.mapAdvice like '%MAPPING GUIDANCE FROM WHO IS AMBIGUOUS%'))
  and a.refSetId = 447562003;
 commit;
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
	 <RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 concat('ExtendedMap: id=',a.id,' : misaligned mapCategory and mapAdvice for Non Classifiable')
 from curr_extendedmaprefset_s a
where a.active = 1
  and (
   (a.mapCategoryId = 447638001 AND a.mapAdvice not like '% CANNOT BE CLASSIFIED WITH AVAILABLE DATA%') or
   (a.mapCategoryId != 447638001 AND a.mapAdvice like '% CANNOT BE CLASSIFIED WITH AVAILABLE DATA%'))
  and a.refSetId = 447562003;
 commit;
 
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,': misaligned mapCategory and mapAdvice for ambiguous snomed concepts')       
 from curr_extendedmaprefset_s a
	where a.active = 1
  	and (
  	 (a.mapCategoryId = 447640006 AND a.mapAdvice not like '%SOURCE SNOMED CONCEPT IS AMBIGUOUS%') or
  	 (a.mapCategoryId != 447640006 AND a.mapAdvice like '%SOURCE SNOMED CONCEPT IS AMBIGUOUS%'))
  	and a.refSetId = 447562003;
 commit;
 
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,': misaligned mapCategory and mapAdvice for incompletely modeled concepts') 
 from curr_extendedmaprefset_s a
where a.active = 1
  and (
   (a.mapCategoryId = 447641005 AND a.mapAdvice not like '%SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED%') or
   (a.mapCategoryId != 447641005 AND a.mapAdvice like '%SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED%'))
 and a.refSetId = 447562003;
 commit;