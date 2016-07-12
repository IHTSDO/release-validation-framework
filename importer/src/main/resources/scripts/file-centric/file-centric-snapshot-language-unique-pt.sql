
/******************************************************************************** 
	file-centric-snapshot-language-unique-pt

	Assertion:
	Every active concept has one and only one active preferred term.

********************************************************************************/
/* 	testing for multiple perferred terms */
/* 	temp table: active sysonyms of active concepts edited in the current release cycle */ 	
	create table if not exists description_tmp as
	select c.*
	from res_concepts_edited a
	join curr_concept_s b 
		on a.conceptid = b.id
	join curr_description_s c
		on b.id = c.conceptid
		and b.active = c.active
		and c.typeid = '900000000000013009' /* synonym */
	where b.active = 1;		

/*  descriptions in the temp table having duplicate preferred language refset members */	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Concept: id=',a.conceptid, ' has multiple active preferred terms.') 
	from description_tmp a
	join curr_langrefset_s b
		on a.id = b.referencedcomponentid
		and b.acceptabilityid = '900000000000548007' /* preferred */
	where a.active = '1'
	group by b.refsetid, b.referencedcomponentid
	having (count(b.refsetid) > 1 and count(b.referencedcomponentid) >1);

/* 	testing for the absence of preferred terms
	make a list of active preferred terms for the active concepts that changed
	in the current release cycle */
	create table if not exists tmp_pt as
	select a.id, a.conceptid, b.refsetid
	from description_tmp a
	join curr_langrefset_s b
		on a.id = b.referencedcomponentid
	where b.active = 1
	and b.acceptabilityid = '900000000000548007'; /* preferred */


	insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' has no active preferred terms in any language refset') 
	from res_edited_active_concepts a
	left join tmp_pt b
		on a.id = b.conceptid
	where b.conceptid is null;
	
	/*  identify concepts that have been edited this cycle, for which there is no 
	US preferred term */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' has no active preferred terms in the US language refset') 
	from res_edited_active_concepts a
	left join 
	(select b.* from tmp_pt b
	where b.refsetid = '900000000000509007') as tmp_us_pt
	on a.id = tmp_us_pt.conceptid
	where tmp_us_pt.conceptid is null;
	
	drop table if exists description_tmp;
	drop table if exists tmp_pt;
	
	commit;
	
	