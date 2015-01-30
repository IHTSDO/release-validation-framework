
/******************************************************************************** 
	component-centric-snapshot-language-active-members

	Assertion:
	Members are active for active descriptions in the snapshot file.

********************************************************************************/
	
/* 	view of current snapshot of language refset referencedcomponentids of active members */
	
	drop table if exists v_act_langrs;
	create table if not exists v_act_langrs (INDEX(referencedcomponentid)) as
	 select referencedcomponentid
		from curr_langrefset_s 
		where active = '1';


 


/* 	view of current snapshot of language refset referencedcomponentids of inactive only members */

	drop table if exists v_inact_only_langrs;
	create table if not exists v_inact_only_langrs (INDEX(referencedcomponentid)) as
	 select a.referencedcomponentid
		from curr_langrefset_s a
		left join v_act_langrs b on a.referencedcomponentid = b.referencedcomponentid
		where a.active = '0'
		and b.referencedcomponentid is null;

	
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		a.id
	
	from curr_description_s a
	inner join curr_concept_s c on a.conceptid = c.id
	inner join v_inact_only_langrs b on a.id = b.referencedComponentId
	where a.active = '1' 
	and c.active = '1';

	drop table if exists v_act_langrs;
	drop table if exists v_inact_only_langrs;

	
