
/******************************************************************************** 
	cs-parser-text-def-existing-missing-rf2

	Assertion:
	TextDef Ids found in CS_ but missing in RF2.

********************************************************************************/
	drop table if exists v_allid;
	drop table if exists v_existingid;
	drop table if exists v_maxidtime;
	drop table if exists v_existingdescription;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists missingrf2existing_tmp;
	drop table if exists exactexistingmatchcsrf2_tmp;

	-- All distinct Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) 
	from cs_description a
	where type_uuid in ('700546a3-09c7-3fc2-9eb9-53d318659a09');


	-- SCTIDs that existed in previous release
	create table if not exists v_existingid as
	select a.* from v_allid a, prev_textdefinition_s b
	where a.id = b.id;

	-- map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime 
	from cs_description
	where type_uuid in ('700546a3-09c7-3fc2-9eb9-53d318659a09')
	group by id; 

	-- all attributes of all previously existing textdef 
	create table if not exists v_existingdescription as
	select a.* 
	from cs_description a, v_existingid b
	where a.id = b.id;

	-- latest attributes of all previously existing textdef in current release
	create table existingmaxattribute_tmp as 
	select a.* 
	from v_existingdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;


	/* Missing in RF2 file */
	create table missingrf2existing_tmp as 
	select a.* 
	from existingmaxattribute_tmp a 
	left join curr_textdefinition_d b 
	on a.id = b.id 
	where b.id is null; 


	-- identify where cs's version for description is same as previous release version for missing RF2 textdef
	create table exactexistingmatchcsrf2_tmp  as 
	select a.* 
	from missingrf2existing_tmp  a 
	inner join prev_textdefinition_s b 
	on a.id = b.id 
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
		concat('TextDef: id=',id, ': TextDef that existed in previous release is referenced in change set file but not in Rf2 file.') 
	from missingrf2existing_tmp;
	
