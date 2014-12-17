
/******************************************************************************** 
	cs-parser-relationship-new-missing-rf2

	Assertion:
	Find new Relationship Ids found is in CS_ but missing in RF2.

********************************************************************************/
	
	
	drop table if exists v_allid;
	drop table if exists v_newid;
	drop table if exists v_maxidtime;
	drop table if exists v_newrelationship;
	drop table if exists newmaxattribute_tmp;
	drop table if exists newinactive_tmp;
	drop table if exists missingrf2new_tmp;
	drop table if exists nonstated_tmp;
	drop table if exists inactiveconcepts_tmp;

	/* Prep */
	-- All distinct Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) from cs_relationship a;

	-- SCTIDs that new to current release
	create table if not exists v_newid as
	select a.* from v_allid a
	left join prev_stated_relationship_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime from cs_relationship 
	group by id; 

	-- All attributes of relationships that are new in current release 
	create table if not exists v_newrelationship as
	select a.* from cs_relationship a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of relationships thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newrelationship a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;




	

	/* Analysis */
	-- CS Relationships that were created in current release but were then inactivated
	create table newinactive_tmp as 
	select * from newmaxattribute_tmp 
	where active = 0;

	-- CS Relationships that are not stated
	create table nonstated_tmp as 
	select * from newmaxattribute_tmp 
	where characteristictypeid != '900000000000010007';

	-- CS Relationships that are stated and define the children of 'Inactive Concept (362955004)'
	create table inactiveconcepts_tmp as 
	select * from curr_stated_relationship_s
	where characteristictypeid = '900000000000010007'
	and typeid = '116680003'
	and destinationid = '362955004';


	-- CS Relationships that were created in current release but are missing from rf2
	create table missingrf2new_tmp as 
	select a.* from newmaxattribute_tmp a 
	left join curr_stated_relationship_d b on a.id = b.id 
	where b.id is null; 
	
	
	
	
	
	
	delete from missingrf2new_tmp
	where id in (
		select id from newinactive_tmp
	);
	
	
	delete from missingrf2new_tmp
	where id in (
		select id from nonstated_tmp
	);
	
	
	delete from missingrf2new_tmp
	where destinationid in (
		select sourceid from inactiveconcepts_tmp
	);
		
		





	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Relationship: id=',id, ': Relationship that is new in current release is referenced in change set file but not in RF2.') 
	from missingrf2new_tmp;
	
	
	
