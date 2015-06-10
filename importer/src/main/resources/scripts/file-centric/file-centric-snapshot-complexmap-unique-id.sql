/*
 * Assert map id is unique in the snapshot file
 */
insert into qa_result (runid, assertionuuid, assertiontext, details)
select
 	<RUNID>,
 	'<ASSERTIONUUID>',
 	'<ASSERTIONTEXT>',
		 concat('ComplexMap: id=',a.id, ' is duplicate in Snapshot file')     
	 from curr_complexmaprefset_s a
	group by a.id
	having count(*) > 1;