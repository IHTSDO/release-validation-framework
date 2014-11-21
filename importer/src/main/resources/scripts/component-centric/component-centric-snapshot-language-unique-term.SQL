
/******************************************************************************** 
	component-centric-snapshot-language-unique-term

	Assertion:
	Descriptions in a language refset are unique.

	limiting the implementation to descriptions associated with concetps edited 
	for the current prospective release

********************************************************************************/
/*	list of active concepts edited for the current prospective release */
	drop temporary table if exists tmp_active_con;
	create temporary table if not exists tmp_active_con as
	select b.*
	from res_concepts_edited a 
		join curr_concept_s b
			on a.conceptid = b.id
			and b.active = 1;
	commit;

/* 	list of refset members per concept, per refset */
	drop temporary table if exists tmp_member;
	create temporary table if not exists tmp_member as
	select a.id as conceptid, c.refsetid, c.referencedcomponentid
	from tmp_active_con a
		join curr_description_s b
			on a.id = b.conceptid
			and a.active = b.active
		join curr_langrefset_s c
			on b.id = c.referencedcomponentid
			and b.active = c.active;
	commit;
	
/* 	violators are descriptions that appear more than once per refset & concept */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',conceptid, ': Concept has duplicate terms within a single language refset.')
	from tmp_member
	group by refsetid, conceptid, referencedcomponentid		
		having count(refsetid) > 1
		and count(conceptid) > 1
		and count(referencedcomponentid) > 1;
	commit;
	
	drop temporary table if exists tmp_active_con;
	drop temporary table if exists tmp_member;

	