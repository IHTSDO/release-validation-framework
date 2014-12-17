
/******************************************************************************** 
	cs-parser-relationship-existing-missing-cs

	Assertion:
	Find existing Relationship Ids found is in RF2 but missing in CS_.

********************************************************************************/
		
	drop table if exists v_allid;
	drop table if exists v_existingid;
	drop table if exists v_maxidtime;
	drop table if exists v_existingrelationship;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists v_existingrf2;
	drop table if exists v_missingexistingcs;


	/* Prep */
	-- All distinct Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) from cs_relationship a;


	-- SCTIDs that existed in previous release
	create table if not exists v_existingid as
	select a.* from v_allid a, prev_stated_relationship_s b
	where a.id = b.id;

	-- Map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime from cs_relationship 
	group by id; 

	-- All attributes of all previously existing relationships 
	create table if not exists v_existingrelationship as
	select a.* from cs_relationship a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing relationships in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingrelationship a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;



	/* Analysis */
	-- Existing Relationships in RF2 
	create table if not exists v_existingrf2 as
	select a.* from curr_stated_relationship_d a
	inner join prev_stated_relationship_s b on a.id = b.id;

	-- Existing Relationships defined in RF2 but are missing in CS File
	create table if not exists v_missingexistingcs  as
	select a.* from v_existingrf2 a 
	left join existingmaxattribute_tmp b on a.id = b.id 
	where b.id is null;


	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Relationship: id=',id, ': Relationship that existed in previous release is referenced in RF2 but not in change set file.') 
	from v_missingexistingcs;
	
