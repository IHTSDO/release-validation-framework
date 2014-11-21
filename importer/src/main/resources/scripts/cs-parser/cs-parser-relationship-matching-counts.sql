/******************************************************************************** 
	cs-parser-relationship-matching-counts

	Assertion:
	Ensure count of cs-to-rf2 Relationship matches equals the count of max 
	time cs and the count of rf2 .

********************************************************************************/
	drop view if exists v_maxidtime;
	drop table if exists maxattribute_tmp;
	drop view if exists v_matching;
	drop view if exists v_cscount;
	drop view if exists v_rf2count;
	drop view if exists v_matchingcount;
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_newrelationship;
	drop table if exists newmaxattribute_tmp;
	drop table if exists newinactive_tmp;
	drop view if exists v_newinactive;
	drop table if exists nonstated_tmp;	
	drop view if exists v_nonstated;
	drop table if exists inactivechildren_tmp;
	drop view if exists v_inactivatedrelationship;
	drop view if exists v_retiredrelationship;	
	drop view if exists v_existingid;
	drop view if exists v_existingrelationship;
	drop table if exists existingmaxattribute_tmp;
	drop table if exists exactexistingmatchcsrf2_tmp;
	drop view if exists v_existingmatch;


	/* Prep */
	-- map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_relationship 
	group by id; 

	-- Latest timestamp of relationships thast are new in current release
	create table maxattribute_tmp as 
	select b.* from v_maxidtime a, cs_relationship b
	where a.id = b.id
	and a.committime = b.committime;

	-- Descriptions whose RF2 Files attributes are exactly the same as the final versions of the CS file
	create view v_matching as 
	select a.id
	from maxattribute_tmp a 
	inner join curr_stated_relationship_d b on a.id = b.id 
	where a.active = b.active 
	and a.sourceid = b.sourceid
	and a.destinationid = b.destinationid
	and a.relationshipgroup = b.relationshipgroup
	and a.typeid = b.typeid
	and a.characteristictypeid = b.characteristictypeid;



	/* Counts */
	create view v_cscount as
	select count(*) as cscount from maxattribute_tmp;

	create view v_rf2count as
	select count(*) as rf2count from curr_stated_relationship_d;

	create view v_matchingcount as
	select count(*) as matchingcount from v_matching;




	/* Subtract from cscount for those Relationships that were created and inactivated in current release */
	-- All distinct Ids in CS
	create view v_allid as
	select distinct(a.id) from cs_relationship a;

	-- SCTIDs that new to current release
	create view v_newid as
	select a.* from v_allid a
	left join prev_stated_relationship_s b on a.id = b.id
	where b.id is null;

	-- All attributes of Relationships that are new in current release 
	create view v_newrelationship as 
	select a.* from cs_relationship a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of Relationships thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newrelationship a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;

	-- CS Stated Relationships that were created in current release but were then inactivated
	create table newinactive_tmp as 
	select * from newmaxattribute_tmp 
	where active = 0;

	-- Generate count of Stated Relationships created and inactivated in current release
	create view v_newinactive as
	select count(*) as newinactive from newinactive_tmp;
	
	
	
	
	
	/* Subtract from cscount for those Relationships that are not stated */
	-- CS Relationships that are not stated
	create table nonstated_tmp as 
	select * from newmaxattribute_tmp 
	where characteristictypeid != '900000000000010007'
	and active = '1';

	-- Generate count of non-stated Relationships 
	create view v_nonstated as
	select count(*) as nonstated from nonstated_tmp;
	
	
	
	
	
	/* Subtract from RF2 for those Relationships that are stated and define the children of 'Inactive Concept (362955004)' */
	-- CS Relationships that are stated and define the children of 'Inactive Concept (362955004)'
	create table inactivechildren_tmp as 
	select * from curr_stated_relationship_s
	where characteristictypeid = '900000000000010007'
	and typeid = '116680003'
	and destinationid = '362955004'
	and active = '1';

	create view v_inactivatedrelationship as
	select * from maxattribute_tmp
	where characteristictypeid = '900000000000010007'
	and typeid = '116680003'
	and active = '1'
	and destinationid in (
		select sourceid from inactivechildren_tmp
	);
	
	-- Generate count of Relationships that are stated and define the children of 'Inactive Concept (362955004)
	create view v_retiredrelationship as
	select count(*) as retiredrelationship from v_inactivatedrelationship;
	





	/* Subtract from cscount for those existing Relationships whose end version is the same as the previously released version */
	-- SCTIDs that existed in previous release
	create view v_existingid as
	select a.* from v_allid a, prev_stated_relationship_s b
	where a.id = b.id;

	-- All attributes of all previously existing Relationships 
	create view v_existingrelationship as
	select a.* from cs_relationship a, v_existingid b
	where a.id = b.id;

	-- Latest attributes of all previously existing Relationships in current release
	create table existingmaxattribute_tmp as 
	select a.* from v_existingrelationship a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;

	-- Existing CS Relationships whose final version is same as the previously release version as defined in the RF2 file
	create table exactexistingmatchcsrf2_tmp  as 
	select a.* from existingmaxattribute_tmp  a 
	inner join prev_stated_relationship_s b on a.id = b.id 
	where b.active = a.active 
	and a.sourceid = b.sourceid
	and a.destinationid = b.destinationid
	and a.relationshipgroup = b.relationshipgroup
	and a.typeid = b.typeid
	and a.characteristictypeid = b.characteristictypeid;

	-- Generate count of existing CS file's Relationships whose final versions are the same as the previously released versions
	create view v_existingmatch as
	select count(*) as existingmatch from exactexistingmatchcsrf2_tmp;





	/* Analysis */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('The number of matched Relationships (' , a.matchingcount, ') does not equal the number of concepts (', b.cscount - c.newinactive - d.nonstated - e.existingmatch - f.retiredrelationship, ') defined in the change set file') 	
	from v_matchingcount a, v_cscount b,
	v_newinactive c, v_nonstated d, 
	v_existingmatch e, v_retiredrelationship f
	where a.matchingcount != (b.cscount - c.newinactive - d.nonstated - e.existingmatch - f.retiredrelationship);
	
	
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('The number of matched Relationships (' , a.matchingcount, ') does not equal the number of concepts (', b.rf2count, ') defined in the Rf2') 	
	from v_matchingcount a, v_rf2count b
	where a.matchingcount != b.rf2count;
	
