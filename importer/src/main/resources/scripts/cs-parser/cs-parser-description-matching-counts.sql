/******************************************************************************** 
	cs-parser-description-matching-counts

	Assertion:
	Ensure count of cs-to-rf2 Description matches equals the count of max 
	time cs and the count of rf2 .

********************************************************************************/
	drop view if exists v_maxidtime;
	drop table if exists maxattribute_tmp;
	drop table if exists matching_tmp;
	drop view if exists v_cscount;
	drop view if exists v_rf2count;
	drop view if exists v_matchingcount;
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_newdescription;
	drop table if exists newmaxattribute_tmp;
	drop table if exists newinactive_tmp;
	drop view if exists v_newinactive;
	drop table if exists textdefintion_tmp;
	drop view if exists v_textdefintion;
	drop view if exists v_existingid;
	drop view if exists v_existingdescription;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists exactexistingmatchcsrf2_tmp;
	drop view if exists v_existingmatch;
	drop view if exists v_allconceptid;
	drop view if exists v_newconceptid;
	drop view if exists v_maxidconcepttime;
	drop view if exists v_newconcept;
	drop table if exists newmaxconceptattribute_tmp;
	drop table if exists newinactiveconcept_tmp;
	drop table if exists newdescriptioninactiveconcept_tmp;
	drop view if exists v_newdescriptioninactiveconcept;


	/* Prep */
	-- map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_description 
	group by id; 

	-- Latest timestamp of Descriptions thast are new in current release
	create table maxattribute_tmp as 
	select b.* from v_maxidtime a, cs_description b
	where a.id = b.id
	and a.committime = b.committime;

	-- Descriptions whose RF2 Files attributes are exactly the same as the final versions of the CS file
	create table matching_tmp as 
	select a.id
	from maxattribute_tmp a 
	inner join curr_description_d b on a.id = b.id 
	where a.active = b.active 
	and a.conceptid = b.conceptid
	and a.languagecode = b.languagecode
	and a.typeid = b.typeid
	and a.term = b.term
	and a.casesignificanceid = b.casesignificanceid;



	/* Counts */
	create view v_cscount as
	select count(*) as cscount from maxattribute_tmp;

	create view v_rf2count as
	select count(*) as rf2count from curr_description_d;

	create view v_matchingcount as
	select count(*) as matchingcount from matching_tmp;




	
	/* Subtract from cscount for those Descriptions that were created and inactivated in current release */
	-- All distinct Descriptions Ids in CS
	create view v_allid as
	select distinct(a.id) from cs_description a;


	-- SCTIDs that new to current release
	create view v_newid as
	select a.* from v_allid a
	left join prev_description_s b on a.id = b.id
	where b.id is null;

	-- All attributes of Descriptions that are new in current release 
	create view v_newdescription as 
	select a.* from cs_description a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of Descriptions thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;

	-- New Descriptions that were created in current release but were then inactivated
	create table newinactive_tmp as 
	select * from newmaxattribute_tmp where active = 0;

	-- Generate count of Descriptions created and inactivated in current release
	create view v_newinactive as
	select count(*) as newinactive from newinactive_tmp;





	/* Subtract from cscount for those TextDefinitions that were created in current release */
	-- List of New Text Definitions in CS Files
	create table textdefintion_tmp as 
	select * from newmaxattribute_tmp 
	where typeid = '900000000000550004'
	and active = '1';

	-- Generate count of TextDefinitions created in current release
	create view v_textdefintion as
	select count(*) as textdefintion from textdefintion_tmp;


	

	/* Subtract from cscount for those existing Descriptions whose end version is the same as the previously released version */
	-- SCTIDs that existed in previous release
	create view v_existingid as
	select a.* from v_allid a, prev_description_s b
	where a.id = b.id;

	-- All attributes of all previously existing Descriptions 
	create view v_existingdescription as
	select a.* from cs_description a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing Descriptions in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;
	
	-- Descriptions where CS file's version is same as previous release version for missing RF2 concepts due to multiple versions within CS File
	create table exactexistingmatchcsrf2_tmp  as 
	select a.* from existingmaxattribute_tmp  a 
	inner join prev_description_s b on a.id = b.id 
	where b.active = a.active 
	and a.conceptid = b.conceptid
	and a.languagecode = b.languagecode
	and a.typeid = b.typeid
	and a.term = b.term
	and a.casesignificanceid = b.casesignificanceid;

	-- Generate count of existing CS file's Descriptions whose final versions are the same as the previously released versions
	create view v_existingmatch as
	select count(*) as existingmatch from exactexistingmatchcsrf2_tmp;





	/* Subtract from cscount for those new Descriptions inactivated Concepts */
	-- All distinct Ids in CS
	create view v_allconceptid as
	select distinct(a.id) from cs_concept a;


	-- SCTIDs that new to current release
	create view v_newconceptid as
	select a.* from v_allconceptid a
	left join prev_concept_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create view v_maxidconcepttime as
	select id, max(committime) as committime from cs_concept 
	group by id; 

	-- All attributes of concepts that are new in current release 
	create view v_newconcept as 
	select a.* from cs_concept a, v_newconceptid b 
	where a.id = b.id;  

	-- Latest timestamp of concepts thast are new in current release
	create table newmaxconceptattribute_tmp as 
	select a.* from v_newconcept a, v_maxidconcepttime b
	where a.id = b.id
	and a.committime = b.committime;

	-- Concepts that were created in current release but were then inactivated
	create table newinactiveconcept_tmp as 
	select * from newmaxconceptattribute_tmp where active = 0;

	-- Descriptions that belong to new concepts that have been inactivated
	create table newdescriptioninactiveconcept_tmp as
	select a.* from maxattribute_tmp a, newinactiveconcept_tmp b
	where a.conceptid = b.id
	and a.active = '1'
	and typeid != '900000000000550004';

	-- Generate count of new CS file's Descriptions that belong to new concepts that have been inactivated
	create view v_newdescriptioninactiveconcept as
	select count(*) as newdescriptioninactiveconcept from newdescriptioninactiveconcept_tmp;
	







	/* Analysis */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('The number of matched Descriptions (' , a.matchingcount, ') does not equal the number of concepts (', b.cscount - c.newinactive - d.textdefintion - e.existingmatch - f.newdescriptioninactiveconcept, ') defined in the change set file') 	
	from v_matchingcount a, v_cscount b,
	v_newinactive c, v_textdefintion d, 
	v_existingmatch e, v_newdescriptioninactiveconcept f
	where a.matchingcount != (b.cscount - c.newinactive - d.textdefintion - e.existingmatch - f.newdescriptioninactiveconcept);

	

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('The number of matched Descriptions (' , a.matchingcount, ') does not equal the number of concepts (', b.rf2count, ') defined in the Rf2') 	
	from v_matchingcount a, v_rf2count b
	where a.matchingcount != b.rf2count;
	
