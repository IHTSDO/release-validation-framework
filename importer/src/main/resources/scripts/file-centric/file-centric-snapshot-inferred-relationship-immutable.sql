
/******************************************************************************** 
	file-centric-snapshot-inferred-relationship-immutable

	Assertion:
	There is a 1:1 relationship between the id and the immutable values in Inferred Relationship snapshot

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.id,
		concat('CONCEPT: id=',b.id, ' contains two or more version of the immutable values in Inferred Relationship Snapshot.') 	

	from curr_relationship_s a 
	inner join curr_concept_s b on a.sourceid = b.id
	where a.active = '1'
	and b.active = '1'
	group by a.id , a.sourceid , a.typeid , a.destinationid
	having count(a.id) > 1;
