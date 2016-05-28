
/******************************************************************************** 
	file-centric-snapshot-concept-successive-states

	Assertion:	
	New inactive states follow active states in the CONCEPT snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ':New inactive states does not follow active states in the concept snapshot.') 	
	from curr_concept_s a inner join  prev_concept_s b
	on a.id = b.id
	where 
	a.active = 0
	and a.active = b.active
	and a.effectivetime != b.effectivetime;

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ': is inactive but no active state found in the previous release.') 	
	from curr_concept_s a left join prev_concept_s b
	on a.id = b.id
	where a.active=0 and b.id is null;
	commit;