
/******************************************************************************** 
	component-centric-snapshot-language-active-members

	Assertion:
	Members are active for active descriptions in the snapshot file.

********************************************************************************/

/* 	view of current snapshot of language refset referencedcomponentids of active members */
	
	drop table if exists v_act_langrs;
	create table if not exists v_act_langrs (INDEX(referencedcomponentid)) as
	 select distinct referencedcomponentid
		from curr_langrefset_s 
		where active = '1';

/* 	view of current snapshot of language refset referencedcomponentids of inactive only members */

	drop table if exists v_inact_only_langrs;
	create table if not exists v_inact_only_langrs (INDEX(referencedcomponentid)) as
	 select distinct a.referencedcomponentid
		from curr_langrefset_s a
		left join v_act_langrs b on a.referencedcomponentid = b.referencedcomponentid
		where a.active = '0'
		and b.referencedcomponentid is null;
	
	/* Finding active descriptions which have NO ACTIVE langrefset */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.id,
		concat('Active description id=',a.id, ' has no active langrefset member') 
	from curr_description_s a
	inner join curr_concept_s c on a.conceptid = c.id
	inner join v_inact_only_langrs b on a.id = b.referencedComponentId
	where a.active = '1' 
	and c.active = '1';

	/* Finding active descriptions which have NO langrefset */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.id,
		concat('Active description id=',a.id, ' has no langrefset member') 
	from curr_description_s a
	inner join curr_concept_s c on a.conceptid = c.id
	left join (select distinct referencedcomponentid from curr_langrefset_s) b on a.id = b.referencedComponentId
	where a.active = '1' 
	and c.active = '1'
	and b.referencedComponentId is null;
	
	drop table if exists v_act_langrs;
	drop table if exists v_inact_only_langrs;
	