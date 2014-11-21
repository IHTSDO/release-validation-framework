
/******************************************************************************** 
	cs-parser-description-existing-missing-cs

	Assertion:
	Find existing Description Ids found is in RF2 but missing in CS_.

********************************************************************************/
		
	drop view if exists v_allid;
	drop view if exists v_existingid;
	drop view if exists v_maxidtime;
	drop view if exists v_existingdescription;
	drop table if exists existingmaxattribute_tmp;
	drop view if exists v_existingrf2;
	drop view if exists v_missingexistingcs;


	/* Prep */
	-- All distinct Ids in CS
	create view v_allid as
	select distinct(a.id) from cs_description a;


	-- SCTIDs that existed in previous release
	create view v_existingid as
	select a.* from v_allid a, prev_description_s b
	where a.id = b.id;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_description 
	group by id; 

	-- All attributes of all previously existing Descriptions 
	create view v_existingdescription as
	select a.* from cs_description a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing Descriptions in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;



	/* Analysis */
	-- Existing Descriptions in RF2 
	create view v_existingrf2 as
	select a.* from curr_description_d a
	inner join prev_description_s b on a.id = b.id;

	-- Existing Descriptions defined in RF2 but are missing in CS File
	create view v_missingexistingcs  as 
	select a.* from v_existingrf2 a 
	left join existingmaxattribute_tmp b on a.id = b.id 
	where b.id is null;


	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Description: id=',id, ': Description that existed in previous release is referenced in RF2 but not in change set file.') 
	from v_missingexistingcs;
	
