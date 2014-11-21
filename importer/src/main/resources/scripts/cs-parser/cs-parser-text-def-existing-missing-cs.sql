
/******************************************************************************** 
	cs-parser-text-def-existing-missing-cs

	Assertion:
	TextDef Ids found in RF2 but missing in CS_.

********************************************************************************/

	drop view if exists v_allid;
	drop view if exists v_existingid;
	drop view if exists v_maxidtime;
	drop view if exists v_existingdescription;
	drop table if exists existingmaxattribute_tmp;
	drop view if exists v_existingrf2;
	drop view if exists v_missingexistingcs;


	-- All distinct Ids in CS
	create view v_allid as
	select distinct(a.id) 
	from cs_description a
	where a.type_uuid ='00791270-77c9-32b6-b34f-d932569bd2bf';


	-- SCTIDs that existed in previous release
	create view v_existingid as
	select a.* 
	from v_allid a, prev_textdefinition_s b
	where a.id = b.id;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime 
	from cs_description
	where type_uuid ='00791270-77c9-32b6-b34f-d932569bd2bf'
	group by id; 

	-- All attributes of all previously existing descriptions 
	create view v_existingdescription as
	select a.* 
	from cs_description a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing descriptions in current release
	create table existingmaxattribute_tmp as 
	select a.* 
	from v_existingdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;


	-- Missing in RF2 file
	create view v_existingrf2 as
	select a.* 
	from curr_textdefinition_d a
	inner join prev_textdefinition_s b 
	on a.id = b.id;

	create view v_missingexistingcs  as 
	select a.* 
	from v_existingrf2 a 
	left join existingmaxattribute_tmp b 
	on a.id = b.id 
	where b.id is null;


	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TextDef: id=',id, ': TextDef that existed in previous release is referenced in RF2 but not in change set file.')
		from v_missingexistingcs;
	
