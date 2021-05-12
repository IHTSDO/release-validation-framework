
/******************************************************************************** 
	file-centric-snapshot-stated-relationship-immutable

	Assertion:
	There is a 1:1 relationship between the id and the immutable values in STATED RELATIONSHIP snapshot

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.id,
		concat('CONCEPT: id=',b.id, ': Concept contains two or more versions of the immutable values in Stated Relationships.'),
		b.id,
		'curr_concept_s'
	from curr_stated_relationship_s a 
	inner join curr_concept_s b on a.sourceid = b.id
	where a.active = '1'
	and b.active = '1'
	group by a.id , a.sourceid , a.typeid , a.destinationid
	having count(a.id) > 1;
