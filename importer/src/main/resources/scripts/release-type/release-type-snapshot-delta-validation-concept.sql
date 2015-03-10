/******************************************************************************** 

	PRIOR RELEASE FROM CURRENT RELEASE FULL FILE
	release-type-snapshot-validation-concept-delta

  
	Assertion:
	The current data in the Concept snapshot file are the same as the data in 
	the current delta file.  

	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.


********************************************************************************/
	
	
	/* selecting the latest components (i.e. the delta) from the snapshot */
  drop table if exists ss;
  create table if not exists ss like curr_concept_s;
  insert into ss
	select *
	from curr_concept_s
	where cast(effectivetime as datetime) >= 
		(select min(cast(effectivetime as datetime))
		 from curr_concept_d);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept in snapshot file, but not in delta file.') 	
	from ss a
	left join curr_concept_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.definitionstatusid = b.definitionstatusid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.definitionstatusid is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept in delta but not in snapshot file.') 	
	from curr_concept_d a
	left join ss b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.definitionstatusid = b.definitionstatusid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.definitionstatusid is null;


	drop table if exists ss;
	 
	 
	 
	 