
/******************************************************************************** 
	file-centric-snapshot-root-concept-has-non-isa-relationships

	Assertion:
	Root concept has non-isa relationships.

********************************************************************************/
	
insert into qa_result (runid, assertionuuid, concept_id, details)
	select distinct
		<RUNID>,
		'<ASSERTIONUUID>',
		r.sourceid,
		concat('ROOT CONCEPT: ',r.sourceid, ' has non-isa relationship id=', r.id, ' with typeid=', r.typeid)
		from curr_relationship_s r 
		where r.sourceid in 
			(select c.id from curr_concept_s c
				where not exists 
					(select r.id from curr_relationship_s s 
						where s.sourceid = c.id
							and s.active = 1
							and s.typeid = 116680003)
				and c.active = 1)
			and r.active = 1;
