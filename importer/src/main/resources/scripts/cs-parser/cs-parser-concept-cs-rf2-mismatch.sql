/******************************************************************************** 
	cs-parser-concept-cs-rf2-mismatch

	Assertion:
	Concept that has different results in change set compared to Rf2.

********************************************************************************/
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_maxidtime;
	drop view if exists v_maxcs_concept;
	drop view if exists v_mismatching;



	/* Prep */
	-- All distinct ids in CS
	create view v_allid as
	select distinct(a.id) from cs_concept a;


	-- SCTids that new to current release
	create view v_newid as
	select a.* from v_allid a
	left join prev_concept_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_concept 
	group by id; 

	
	-- Latest attributes of all concepts in current release
	create view v_maxcs_concept as
	select a.* from cs_concept a, v_maxidtime b
	where a.id = b.id 
	and a.committime = b.committime;



	/* Analysis */
	-- Concepts that have different values in CS files compared to RF2
	create view v_mismatching as 
	select a.id, a.concept_uuid,
			a.active as cs_active, 
			a.definitionstatusid as cs_definitionstatusid,
			b.active as rf2_active,
			b.definitionstatusid as rf2_definitionstatusid
	from v_maxcs_concept a 
	inner join curr_concept_d b on a.id = b.id 
	where a.active != b.active 
	or a.definitionstatusid != b.definitionstatusid;



	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Concept: id=',id, ': Concept that has different results in change set compared to Rf2.') 
	from v_mismatching;
	
