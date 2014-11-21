
/******************************************************************************** 
	cs-parser-concept-existing-missing-rf2

	Assertion:
	Find existing Concept Ids found is in CS_ but missing in RF2.

********************************************************************************/
	drop view if exists v_allid;
	drop view if exists v_existingid;
	drop view if exists v_maxidtime;
	drop view if exists v_existingconcept;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists missingrf2existing_tmp;
	drop table if exists exactexistingmatchcsrf2_tmp;

	/* Prep */
	-- All distinct Ids in CS
	create view v_allid as
	select distinct(a.id) from cs_concept a;


	-- SCTIDs that existed in previous release
	create view v_existingid as
	select a.* from v_allid a, prev_concept_s b
	where a.id = b.id;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_concept 
	group by id; 

	-- All attributes of all previously existing concepts 
	create view v_existingconcept as
	select a.* from cs_concept a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing concepts in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingconcept a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;



	/* Analysis */
	-- Existing Concepts that are in the CS file but missing in the RF2 file
	create table missingrf2existing_tmp as 
	select a.* from existingmaxattribute_tmp a 
	left join curr_concept_d b on a.id = b.id 
	where b.id is null; 


	-- Concepts where CS file's version is same as previous release version for missing RF2 concepts due to multiple versions within CS File
	create table exactexistingmatchcsrf2_tmp  as 
	select a.* from missingrf2existing_tmp  a 
	inner join prev_concept_s b on a.id = b.id 
	where b.active = a.active 
	and a.definitionstatusid = b.definitionstatusid;


	-- Remove exact matches
	delete from missingrf2existing_tmp
	where id in (
		select id from exactexistingmatchcsrf2_tmp
	); 


 

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Concept: id=',id, ': Concept that existed in previous release is referenced in change set file but not in Rf2 file.') 
	from missingrf2existing_tmp;
	
	
	
	
