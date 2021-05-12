
/******************************************************************************** 
	component-centric-snapshot-language-unique-term

	Assertion:
	Descriptions in a language refset are unique.

	limiting the implementation to descriptions associated with concetps edited 
	for the current prospective release

********************************************************************************/

/* 	list of refset members per concept, per refset */
	drop table if exists tmp_member;
	create table if not exists tmp_member as
	select a.id as conceptid, c.refsetid, c.referencedcomponentid
	from res_edited_active_concepts a
		join curr_description_s b
			on a.id = b.conceptid
			and a.active = b.active
		join curr_langrefset_s c
			on b.id = c.referencedcomponentid
			and b.active = c.active;
	commit;
	
/* 	violators are descriptions that appear more than once per refset & concept */
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		conceptid,
		concat('CONCEPT: id=',conceptid, ': Concept has duplicate terms within a single language refset.'),
		conceptid,
		'curr_concept_s'
	from tmp_member
	group by refsetid, conceptid, referencedcomponentid		
		having count(refsetid) > 1
		and count(conceptid) > 1
		and count(referencedcomponentid) > 1;
	commit;
	
	drop table if exists v_tmp_active_con;
	drop table if exists tmp_member;

	