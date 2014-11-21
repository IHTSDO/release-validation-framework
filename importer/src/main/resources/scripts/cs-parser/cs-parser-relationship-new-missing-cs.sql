/******************************************************************************** 
	cs-parser-relationship-new-missing-cs

	Assertion:
	Find new Relationship Ids found is in RF2 but missing in CS_.

********************************************************************************/
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_maxidtime;
	drop view if exists v_newrelationship;
	drop table if exists newmaxattribute_tmp;
	drop view if exists v_newrf2;
	drop view if exists v_missingnewcs;


	/* Prep */
	-- All distinct Ids in CS
	create view v_allid as
	select distinct(a.id) from cs_relationship a;


	-- SCTIDs that new to current release
	create view v_newid as
	select a.* from v_allid a
	left join prev_stated_relationship_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_relationship 
	group by id; 

	-- All attributes of relationships that are new in current release 
	create view v_newrelationship as 
	select a.* from cs_relationship a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of relationships thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newrelationship a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;




	

	/* Analysis */
	-- New Rf2 Relationships that were created in current release 
	create view v_newrf2 as
	select a.* from curr_stated_relationship_d a 
	left join prev_stated_relationship_s b on a.id = b.id
	where b.id is null;


	-- New Rf2 Relationships that were created in current release but are missing from CS File
	create view v_missingnewcs  as 
	select a.* from v_newrf2 a 
	left join newmaxattribute_tmp b on a.id = b.id 
	where b.id is null;
	




	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Relationship: id=',id, ': Relationship that is new in current release is referenced in RF2 but not in change set file.') 
	from v_missingnewcs;
	
