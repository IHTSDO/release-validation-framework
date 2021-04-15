
/******************************************************************************** 
	file-centric-snapshot-root-concept-has-non-isa-relationships

	Assertion:
	Root concept has non-isa relationships.

********************************************************************************/
	
insert into qa_result (runid, assertionuuid, concept_id, details)
	select distinct
		<RUNID>,
		'<ASSERTIONUUID>',
		r.destinationid,
		concat('ROOT CONCEPT: ',r.destinationid, 'has non-isa relationship: ', r.typeid)
		from curr_relationship_s r 
		where r.destinationid in 
			(select c.id from curr_concept_s c
				where not exists 
					(select r.id from curr_relationship_s s 
						where s.sourceid = c.id
							and s.active = 1)
				and c.active = 1)
			and r.active = 1
			and r.typeid != 116680003;