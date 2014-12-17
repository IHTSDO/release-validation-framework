/******************************************************************************** 
	cs-parser-concept-cs-rf2-mismatch

	Assertion:
	Ensure count of cs-to-rf2 Concept matches equals the count of max 
	time cs and the count of rf2 .
.

********************************************************************************/
	drop table if exists v_allid;
	drop table if exists v_maxidtime;
	drop table if exists maxattribute_tmp;
	drop table if exists v_matching;
	drop table if exists v_cscount;
	drop table if exists v_rf2count;
	drop table if exists v_newid;
	drop table if exists v_matchingcount;
	drop table if exists v_newconcept;
	drop table if exists newmaxattribute_tmp;
	drop table if exists newinactive_tmp;
	drop table if exists v_newinactive;
	drop table if exists v_existingid;
	drop table if exists v_existingconcept;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists exactexistingmatchcsrf2_tmp;
	drop table if exists v_existingmatch;


	/* Prep */
	-- All distinct Concept Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) from cs_concept a;

	-- map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime from cs_concept 
	group by id; 

	-- Latest timestamp of Concepts thast are new in current release
	create table maxattribute_tmp as 
	select b.* from v_maxidtime a, cs_concept b
	where a.id = b.id
	and a.committime = b.committime;

	-- Concepts whose RF2 Files attributes are exactly the same as the final versions of the CS file
	create table if not exists v_matching as
	select a.id
	from maxattribute_tmp a 
	inner join curr_concept_d b on a.id = b.id 
	where a.active = b.active 
	and a.definitionstatusid = b.definitionstatusid;



	/* Counts */
	create table if not exists v_cscount as
	select count(*) as cscount from maxattribute_tmp;

	create table if not exists v_rf2count as
	select count(*) as rf2count from curr_concept_d;

	create table if not exists v_matchingcount as
	select count(*) as matchingcount from v_matching;




	/* Subtract from cscount for those Concepts that were created and inactivated in current release */
	-- SCTIDs that new to current release
	create table if not exists v_newid as
	select a.* from v_allid a
	left join prev_concept_s b on a.id = b.id
	where b.id is null;

	-- All attributes of Concepts that are new in current release 
	create table if not exists v_newconcept as
	select a.* from cs_concept a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of Concepts thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newconcept a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;
	 
	-- Concepts that were created in current release but were then inactivated
	create table newinactive_tmp as 
	select * from newmaxattribute_tmp where active = 0;

	-- Generate count of Concepts created and inactivated in current release
	create table if not exists v_newinactive as
	select count(*) as newinactive from newinactive_tmp;



	/* Subtract from cscount for those existing Concepts whose end version is the same as the previously released version */
	-- SCTIDs that existed in previous release
	create table if not exists v_existingid as
	select a.* from v_allid a, prev_concept_s b
	where a.id = b.id;

	-- All attributes of all previously existing Concepts 
	create table if not exists v_existingconcept as
	select a.* from cs_concept a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing Concepts in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingconcept a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;

	-- Concepts where CS file's version is same as previous release version for missing RF2 Concepts due to multiple versions within CS File
	create table exactexistingmatchcsrf2_tmp  as 
	select a.* from existingmaxattribute_tmp  a 
	inner join prev_concept_s b on a.id = b.id 
	where b.active = a.active 
	and a.definitionstatusid = b.definitionstatusid;

	-- Generate count of existing CS file's Concepts whose final versions are the same as the previously released versions
	create table if not exists v_existingmatch as
	select count(*) as existingmatch from exactexistingmatchcsrf2_tmp;





	/* Analysis */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('The number of matched Concepts (' , a.matchingcount, ') does not equal the number of concepts (', b.cscount - c.newinactive - d.existingmatch, ') defined in the change set file') 	
	from v_matchingcount a, v_cscount b,
	v_newinactive c, v_existingmatch d
	where a.matchingcount != (b.cscount - c.newinactive - d.existingmatch);
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('The number of matched Concepts (' , a.matchingcount, ') does not equal the number of concepts (', b.rf2count, ') defined in the Rf2') 	
	from v_matchingcount a, v_rf2count b
	where a.matchingcount != b.rf2count;
	
