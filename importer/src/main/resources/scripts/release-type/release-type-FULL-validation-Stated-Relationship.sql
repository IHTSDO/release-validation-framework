
/******************************************************************************** 

	release-type-FULL-validation-Stated-Relationship 

	Assertion:	The current Stated Relationship full file contains all 
	previously published data unchanged.


	The current full file is the same as the prior version of the same full 
	file, except for the delta rows. Therefore, when the delta rows are excluded 
	from the current file, it should be identical to the prior version.

	This test identifies rows in prior, not in current, and in current, not in 
	prior.

	select * from curr_stated_relationship_f where id = 'a'
********************************************************************************/
	

	drop table if exists v_curr_view;
	create table if not exists v_curr_view as
		select *
		from curr_stated_relationship_f
		where cast(effectivetime as datetime) <
			(select max(cast(effectivetime as datetime)) 
			 from curr_stated_relationship_f);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('stated relationship id=',a.id, '; stated relationship in current release file, but not in prior release file.') 	
	from v_curr_view a
	left join prev_stated_relationship_f b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.sourceid = b.sourceid
		and a.destinationid = b.destinationid
		and a.relationshipgroup = b.relationshipgroup
		and a.typeid = b.typeid
		and a.characteristictypeid = b.characteristictypeid
		and a.modifierid = b.modifierid	
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.sourceid is null
	or b.destinationid is null
	or b.relationshipgroup is null
	or b.typeid is null
	or b.characteristictypeid is null
	or b.modifierid is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('stated relationship: id=',a.id, '; relationship in prior release file but not in current release file.') 	
	from prev_stated_relationship_f a
	left join v_curr_view b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.sourceid = b.sourceid
		and a.destinationid = b.destinationid
		and a.relationshipgroup = b.relationshipgroup
		and a.typeid = b.typeid
		and a.characteristictypeid = b.characteristictypeid
		and a.modifierid = b.modifierid	
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.sourceid is null
	or b.destinationid is null
	or b.relationshipgroup is null
	or b.typeid is null
	or b.characteristictypeid is null
	or b.modifierid is null;

  truncate table v_curr_view;
  drop table if exists v_curr_view;

	commit;	
	
	