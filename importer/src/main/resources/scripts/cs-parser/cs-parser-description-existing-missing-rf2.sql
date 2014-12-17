
/******************************************************************************** 
	cs-parser-description-existing-missing-rf2

	Assertion:
	Find existing Description Ids found is in CS_ but missing in RF2.

********************************************************************************/
	drop table if exists v_allid;
	drop table if exists v_existingid;
	drop table if exists v_maxidtime;
	drop table if exists v_existingdescription;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists missingrf2existing_tmp;
	drop table if exists exactexistingmatchcsrf2_tmp;

	/* Prep */
	-- All distinct Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) from cs_description a;


	-- SCTIDs that existed in previous release
	create table if not exists v_existingid as
	select a.* from v_allid a, prev_description_s b
	where a.id = b.id;

	-- Map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime from cs_description 
	group by id; 

	-- All attributes of all previously existing Descriptions 
	create table if not exists v_existingdescription as
	select a.* from cs_description a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing Descriptions in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;



	/* Analysis */
	-- Existing Descriptions found in CS file but are missing in RF2 file
	create table missingrf2existing_tmp as 
	select a.* from existingmaxattribute_tmp a 
	left join curr_description_d b on a.id = b.id 
	where b.id is null; 


	-- Existing Descriptions whose latest version in CS file is same as previously released RF2 descriptions
	create table exactexistingmatchcsrf2_tmp  as 
	select a.* from missingrf2existing_tmp  a 
	inner join prev_description_s b on a.id = b.id 
	where b.active = a.active 
	and a.conceptid = b.conceptid
	and a.languagecode = b.languagecode
	and a.typeid = b.typeid
	and a.term = b.term
	and a.casesignificanceid = b.casesignificanceid;


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
		concat('Description: id=',id, ': Description that existed in previous release is referenced in change set file but not in Rf2 file.') 
	from missingrf2existing_tmp;
	
	
	
	
