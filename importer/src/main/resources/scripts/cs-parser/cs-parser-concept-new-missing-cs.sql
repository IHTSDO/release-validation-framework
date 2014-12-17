/******************************************************************************** 
	cs-parser-concept-new-missing-cs

	Assertion:
	Find new Concept Ids found is in RF2 but missing in CS_.

********************************************************************************/
	drop table if exists v_allid;
	drop table if exists v_newid;
	drop table if exists v_maxidtime;
	drop table if exists v_newconcept;
	drop table if exists newmaxattribute_tmp;
	drop table if exists v_newrf2;
	drop table if exists v_missingnewcs;


	/* Prep */
	-- All distinct Ids in CS
	create table if not exists v_allid as
	select distinct(a.id) from cs_concept a;


	-- SCTIDs that new to current release
	create table if not exists v_newid as
	select a.* from v_allid a
	left join prev_concept_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create table if not exists v_maxidtime as
	select id, max(committime) as committime from cs_concept 
	group by id; 

	-- All attributes of concepts that are new in current release 
	create table if not exists v_newconcept as
	select a.* from cs_concept a, v_newid b 
	where a.id = b.id;  

	-- Latest timestamp of concepts thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* from v_newconcept a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;




	

	/* Analysis */
	-- RF2 Concepts that are new to current release
	create table if not exists v_newrf2 as
	select a.* from curr_concept_d a 
	left join prev_concept_s b on a.id = b.id
	where b.id is null;


	-- New Concepts that are in RF2 Files but missing in CS Files
	create table if not exists v_missingnewcs  as
	select a.* from newrf2 a 
	left join newmaxattribute_tmp b on a.id = b.id 
	where b.id is null;
	




	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Concept: id=',id, ': Concept that is new in current release is referenced in RF2 but not in change set file.') 
	from v_missingnewcs;
	
