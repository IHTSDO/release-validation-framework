/******************************************************************************** 
	cs-parser-description-cs-rf2-mismatch

	Assertion:
	Description that has different results in change set compared to Rf2.

********************************************************************************/
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_maxidtime;
	drop view if exists v_maxcs_description;
	drop view if exists v_mismatching;



	/* Prep */
	-- All distinct ids in CS 
	create view v_allid as
	select distinct(a.id) from cs_description a;


	-- SCTids that new to current release
	create view v_newid as
	select a.* from v_allid a 
	left join prev_description_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_description 
	group by id; 

	-- Latest attributes of all previously existing Descriptions in current release
	create view v_maxcs_description as 
	select a.* from cs_description a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;


	/* Analysis */
	-- Descriptions that have different values in CS files compared to RF2
	create view v_mismatching as 
	select a.id, a.description_uuid,
			a.active as cs_active, 
			a.conceptid as cs_conceptid,
			a.languagecode as cs_languagecode,
			a.typeid as cs_typeid,
			a.term as cs_term,
			a.casesignificanceid as cs_casesignificanceid,
			b.active as rf2_active, 
			b.conceptid as rf2_conceptid,
			b.languagecode as rf2_languagecode,
			b.typeid as rf2_typeid,
			b.term as rf2_term,
			b.casesignificanceid as rf2_casesignificanceid
	from v_maxcs_description a 
	inner join curr_description_d b on a.id = b.id 
	where a.active != b.active 
	or a.conceptid != b.conceptid
	or a.languagecode != b.languagecode
	or a.typeid != b.typeid
	or a.term != b.term
	or a.casesignificanceid != b.casesignificanceid;



	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Description: id=',id, ': Description that has different results in change set compared to Rf2.') 
	from v_mismatching;
	
