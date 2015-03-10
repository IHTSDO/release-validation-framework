
/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE 

	Assertion:
	The current Concept full file contains all previously published data 
	unchanged.

	The current fulll file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.


********************************************************************************/
	

	drop table if exists v_curr_view;
  create table if not exists v_curr_view like curr_concept_f;
  insert into v_curr_view
		select *
		from curr_concept_f
		where cast(effectivetime as datetime) <=
			(select max(cast(effectivetime as datetime)) 
			 from prev_concept_f);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept in current release file, but not in prior release file.') 	
	from v_curr_view a
	left join prev_concept_f b
		on a.id = b.id
	where b.id is null
	or b.definitionstatusid is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept in prior release file but not in current release file.') 	
	from prev_concept_f a
	left join v_curr_view b
			on a.id = b.id
	where b.id is null
	or b.definitionstatusid is null;


  truncate table v_curr_view;
	drop table if exists v_curr_view;
