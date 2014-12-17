/******************************************************************************** 
	cs-parser-description-new-missing-cs

	Assertion:
	Find new Description Ids found is in RF2 but missing in CS_.

********************************************************************************/
	drop table if exists v_allid;
	drop table if exists v_newid;
	drop table if exists v_maxidtime;
	drop table if exists v_newdescription;
	drop table if exists newmaxattribute_tmp;
	drop table if exists v_newrf2;
	drop table if exists v_missingnewcs;


	/* Prep */
	-- All distinct Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) from cs_description a;


	-- SCTIDs that new to current release
	create table if not exists v_newid as
	select a.* from v_allid a
	left join prev_description_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime from cs_description 
	group by id; 

	-- All attributes of descriptions that are new in current release 
	create table if not exists v_newdescription as
	select a.* from cs_description a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of descriptions thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;




	

	/* Analysis */
	-- RF2 Descriptions that were created in current release 
	create table if not exists v_newrf2 as
	select a.* from curr_description_d a 
	left join prev_description_s b on a.id = b.id
	where b.id is null;

	-- New Descriptions found in RF2 but are missing from CS File
	create table if not exists v_missingnewcs  as
	select a.* from v_newrf2 a 
	left join newmaxattribute_tmp b on a.id = b.id 
	where b.id is null;
	



	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Description: id=',id, ': Description that is new in current release is referenced in RF2 but not in change set file.') 
	from v_missingnewcs;
	
