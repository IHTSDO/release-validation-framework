/*
 * active extended maps have valid mapRule syntax (may be project-specific)
* requires a parser to be implemented properly.
*active extended maps have mapAdvice restricted to the valid list (may be project specific)
* this is really hard to implement in SQL because the mapAdvice is not a relational field
* In a sense this is really like the prior one, ideally there would be a parser for the field that would check correctness (and  it's also very project specific). 
* One thing we do know, is that any blank mapTarget should also appear in the advice (for ICD10 project)
 */
insert into qa_result (runid, assertionuuid, concept_id, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 concat('ExtendedMap: id=',a.id,' : map target missing from advice:', a.mapTarget)  
 from curr_extendedmaprefset_s a
where a.active = 1
  and a.mapTarget != ''
  and a.mapAdvice not like concat('% ',a.mapTarget,'%')
  and a.refSetId = 447562003;
 commit;
 