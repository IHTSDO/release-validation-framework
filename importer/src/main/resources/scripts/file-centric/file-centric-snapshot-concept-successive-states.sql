
/******************************************************************************** 
	file-centric-snapshot-concept-successive-states

	Assertion:	
	New inactive states follow active states in the CONCEPT snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ':New inactive states follow active states in the CONCEPT snapshot.') 	
	from curr_concept_s a , prev_concept_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_concept_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;
	commit;
