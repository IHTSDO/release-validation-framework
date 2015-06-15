/*
 * ExtendedMap id must be unique in the snapshot file
 */
insert into qa_result (runid, assertionuuid, assertiontext, details)
select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
		 concat('ExtendedMap: id=',a.id, ' is duplicate in Snapshot file')     
	 from curr_extendedmaprefset_s a
	group by a.id
	having count(*) > 1;
	
/*
 * complex/extended map refset member id,effectiveTime tuples are unique
 * NOTE: this is  really a rule that applies to ALL RRF files.
 */
 
insert into qa_result (runid, assertionuuid, assertiontext, details)
select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
		 concat('ExtendedMap: id=',a.id, ', effectiveTime=', a.effectiveTime, ':Duplicate id,effectiveTime')     
	 from curr_extendedmaprefset_f a
	group by a.id, a.effectiveTime
	having count(*) > 1;
commit;
