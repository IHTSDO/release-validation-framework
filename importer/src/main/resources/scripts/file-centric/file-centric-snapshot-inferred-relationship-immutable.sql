
/******************************************************************************** 
	file-centric-snapshot-inferred-relationship-immutable

	Assertion:
	There is a 1:1 relationship between the id and the immutable values in Inferred Relationship snapshot

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',b.id, ': Concept contains two or more version of the immutable values in Inferred Relationships.') 	

	from curr_relationship_s a 
	inner join curr_concept_s b on a.sourceid = b.id
	where a.active = '1'
	and b.active = '1'
	group by a.id , a.sourceid , a.typeid , a.destinationid
	having count(a.id) > 1;
