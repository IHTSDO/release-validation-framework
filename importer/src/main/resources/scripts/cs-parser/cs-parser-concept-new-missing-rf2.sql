
/******************************************************************************** 
	cs-parser-concept-new-missing-rf2

	Assertion:
	Find new Concept Ids found is in CS_ but missing in RF2.

********************************************************************************/
	
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_maxidtime;
	drop view if exists v_newconcept;
	drop table if exists newmaxattribute_tmp;
	drop table if exists newinactive_tmp;
	drop table if exists missingrf2new_tmp;


	/* Prep */
	-- All distinct Ids in CS
	create view v_allid as
	select distinct(a.id) from cs_concept a;


	-- SCTIDs that new to current release
	create view v_newid as
	select a.* from v_allid a
	left join prev_concept_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_concept 
	group by id; 

	-- All attributes of concepts that are new in current release 
	create view v_newconcept as 
	select a.* from cs_concept a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of concepts thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newconcept a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;




	

	/* Analysis */
	-- Concepts that were created in current release but were then inactivated
	create table newinactive_tmp as 
	select * from newmaxattribute_tmp where active = 0;

	-- New Concepts in CS Files but missing in RF2
	create table missingrf2new_tmp as 
	select a.* from newmaxattribute_tmp a 
	left join curr_concept_d b on a.id = b.id 
	where b.id is null; 

	-- Remove from missing list new and inactivated concepts as they reside in CS files but not RF2 Files
	delete from missingrf2new_tmp
	where id in (
		select id from newinactive_tmp
	);
	
	




	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Concept: id=',id, ': Concept that is new in current release is referenced in change set file but not in RF2.') 
	from missingrf2new_tmp;
	
	
	
