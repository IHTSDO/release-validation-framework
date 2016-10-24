/*  
 * There must be actual changes made to previously published concepts in order for them to appear in the current delta.
*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' is in the detla file, but no actual changes made since the previous release.') 	
	from curr_concept_d a
	left join prev_concept_s b
	on a.id = b.id
	and a.active = b.active
	and a.moduleid = b.moduleid
	and a.definitionstatusid = b.definitionstatusid
	where b.id is not null;
	