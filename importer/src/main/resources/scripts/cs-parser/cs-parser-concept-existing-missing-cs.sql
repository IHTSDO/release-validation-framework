
/******************************************************************************** 
	cs-parser-concept-existing-missing-cs

	Assertion:
	Find existing Concept Ids found is in RF2 but missing in CS_.

********************************************************************************/
		
	drop table if exists v_allid;
	drop table if exists v_existingid;
	drop table if exists v_maxidtime;
	drop table if exists v_existingconcept;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists v_existingrf2;
	drop table if exists v_missingexistingcs;


	/* Prep */
	-- All distinct Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) from cs_concept a;


	-- SCTIDs that existed in previous release
	create table if not exists v_existingid as
	select a.* from v_allid a, prev_concept_s b
	where a.id = b.id;

	-- Map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime from cs_concept 
	group by id; 

	-- All attributes of all previously existing concepts 
	create table if not exists v_existingconcept as
	select a.* from cs_concept a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing concepts in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingconcept a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;



	/* Analysis */
	-- RF2 Concepts that existed in previous release
	create table if not exists v_existingrf2 as
	select a.* from curr_concept_d a
	inner join prev_concept_s b on a.id = b.id;

	-- Existed Concepts that are in RF2 Files but missing in CS Files
	create table if not exists v_missingexistingcs  as
	select a.* from v_existingrf2 a 
	left join existingmaxattribute_tmp b on a.id = b.id 
	where b.id is null;


	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Concept: id=',id, ': Concept that existed in previous release is referenced in RF2 but not in change set file.') 
	from v_missingexistingcs;
	
