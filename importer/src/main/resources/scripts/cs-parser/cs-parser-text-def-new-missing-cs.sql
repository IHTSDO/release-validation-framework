/******************************************************************************** 
	cs-parser-text-def-new-missing-rf2

	Assertion:
	New Textdefinition Ids found in RF2 but missing in CS_.

********************************************************************************/
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_maxidtime;
	drop view if exists v_newdescription;
	drop table if exists newmaxattribute_tmp;
	drop view if exists v_newrf2;
	drop view if exists v_missingnewcs;


	/* Prep */
	-- All distinct Ids in CS
	create view v_allid as
	select distinct(a.id) from cs_description a
	where a.type_uuid in ('700546a3-09c7-3fc2-9eb9-53d318659a09');


	-- SCTIDs that new to current release
	create view v_newid as
	select a.* from v_allid a
	left join prev_textdefinition_s b 
	on a.id = b.id
	where b.id is null;

	-- map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime 
	from cs_description
	where type_uuid in ('700546a3-09c7-3fc2-9eb9-53d318659a09')
	group by id; 

	-- all attributes of descriptions that are new in current release 
	create view v_newdescription as 
	select a.* 
	from cs_description a, v_newid b 
	where a.id = b.id
	and type_uuid in ('700546a3-09c7-3fc2-9eb9-53d318659a09');  

	-- Latest timestamp of descriptions thast are new in current release
	create table newmaxattribute_tmp as 
	select a.* 
	from v_newdescription a, v_maxidtime b
	where a.id = b.id
	and a.committime = b.committime;



	/* Analysis */
	-- Descriptions that were created in current release but were then inactivated
	create view v_newrf2 as
	select a.* 
	from curr_textdefinition_s a 
	left join prev_textdefinition_s b 
	on a.id = b.id
	where b.id is null;


	create view v_missingnewcs  as 
	select a.* from v_newrf2 a 
	left join newmaxattribute_tmp b 
	on a.id = b.id 
	where b.id is null;
	


	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Description: id=',id, ': Description that is new in current release is referenced in RF2 but not in change set file.') 
	from v_missingnewcs;
	
