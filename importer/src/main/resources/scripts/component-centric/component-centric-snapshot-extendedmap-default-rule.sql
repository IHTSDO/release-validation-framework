/*
 * Active rule based extended map groups should end with a default rule (e.g. mapRule=TRUE,OTHERWISE TRUE)
 * ...list of refsetIds for "rule based" mappings.. ,e.g. 447562003
 */
insert into qa_result (runid, assertionuuid, assertiontext, details)
select
	 <RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id,' does not end with default rule')      
 from curr_extendedmaprefset_s a join
  	(select referencedComponentId, mapGroup,max(mapPriority) as maxPriority from curr_extendedmaprefset_s  group by referencedComponentId, mapGroup ) b
	where a.active = 1
 	and a.mapRule not like '%TRUE'
  	and a.refSetId in ('447562003')
  	and a.referencedComponentId = b.referencedComponentId
  	and a.mapGroup = b.mapGroup
  	and a.mapPriority = b.maxPriority;
 commit; 

/*
 * 
 * I think in the next release we are stopping the practice of inlining descendants, in which case this rule no longer applies.
 * active complex/extended maps that are rule based always have a mapRule value
 * NOTE: theres's no complexmap data for this at the moment, but the same assertion technically applies.
 */
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
 select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
 	concat('ExtendedMap: id=',a.id, ' is a rule based map but with a blank rule')   
 from curr_extendedmaprefset_s a
	where a.active = 1 
  	and a.mapRule = ''
  	and a.refSetId in (select distinct(b.refSetId) from curr_extendedmaprefset_s b
     	where b.active = 1 and mapRule != '');
 commit;
 
 