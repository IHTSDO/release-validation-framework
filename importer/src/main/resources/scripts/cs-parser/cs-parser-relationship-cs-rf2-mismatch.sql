/******************************************************************************** 
	cs-parser-relationship-cs-rf2-mismatch

	Assertion:
	Relationship that has different results in change set compared to Rf2.

********************************************************************************/
	drop view if exists v_allid;
	drop view if exists v_newid;
	drop view if exists v_maxidtime;
	drop view if exists v_maxcs_relationship;
	drop view if exists v_mismatching;



	/* Prep */
	-- All distinct ids in CS
	create view v_allid as
	select distinct(a.id) from cs_relationship a;


	-- SCTids that new to current release
	create view v_newid as
	select a.* from v_allid a
	left join prev_stated_relationship_s b on a.id = b.id
	where b.id is null;

	-- Map all ids to latest committime
	create view v_maxidtime as
	select id, max(committime) as committime from cs_relationship 
	group by id; 

	-- Latest attributes of all previously existing Relationships in current release
	create view v_maxcs_relationship as
	select a.* from cs_relationship a, v_maxidtime b
	where a.id = b.id 
	and a.committime = b.committime;



	/* Analysis */
	-- Relationships found in RF2 that has different values compared to the final version defined in the CS file
	create view v_mismatching as 
	select a.id, a.relationship_uuid,
			a.active as cs_active, 
			a.sourceid as cs_sourceid,
			a.destinationid as cs_destinationid,
			a.relationshipgroup as cs_relationshipgroup,
			a.typeid as cs_typeid,
			a.characteristictypeid as cs_characteristictypeid,
			b.active as rf2_active, 
			b.sourceid as rf2_sourceid,
			b.destinationid as rf2_destinationid,
			b.relationshipgroup as rf2_relationshipgroup,
			b.typeid as rf2_typeid,
			b.characteristictypeid as rf2_characteristictypeid
	from v_maxcs_relationship a 
	inner join curr_stated_relationship_d b on a.id = b.id 
	where a.active != b.active 
	or a.sourceid != b.sourceid
	or a.destinationid != b.destinationid
	or a.relationshipgroup != b.relationshipgroup
	or a.typeid != b.typeid
	or a.characteristictypeid != b.characteristictypeid;



	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Relationship: id=',id, ': Relationship that has different results in change set compared to Rf2.') 
	from v_mismatching;
	
