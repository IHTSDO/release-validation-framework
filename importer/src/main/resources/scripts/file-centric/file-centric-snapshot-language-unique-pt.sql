
/******************************************************************************** 
	file-centric-snapshot-language-unique-pt

	Assertion:
	Every active concept has one and only one active preferred term.

********************************************************************************/
/* 	testing for multiple perferred terms */
/* 	temp table: active sysonyms of active concepts edited in the current release cycle */ 	
	create temporary table if not exists description_tmp as
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
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.conceptid, ': Concept has multiple active preferred terms.') 
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
	create temporary table if not exists tmp_pt as		 
	select a.id, a.conceptid, b.refsetid
	from description_tmp a
	join curr_langrefset_s b
		on a.id = b.referencedcomponentid
	where b.active = 1
	and b.acceptabilityid = '900000000000548007'; /* preferred */

/*  make a list of active concepts that have been edited this release cycle */
	create temporary table if not exists tmp_edited_concept as
	select a.*
	from res_concepts_edited a
	join curr_concept_s b 
		on a.conceptid = b.id
	where b.active = 1;	

/*  identify concepts that have been edited this cycle, for which there is no 
	US preferred term */
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.conceptid, ': Concept has no active preferred term.') 
	from tmp_edited_concept a
	left join tmp_pt b
		on a.conceptid = b.conceptid
	where b.conceptid is null
	and b.refsetid = '900000000000509007'; /* US lang refset */

/*  identify concepts that have been edited this cycle, for which there is no 
	GB preferred term */
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.conceptid, ': Concept has no active preferred term.') 
	from tmp_edited_concept a
	left join tmp_pt b
		on a.conceptid = b.conceptid
	where b.conceptid is null
	and b.refsetid = '900000000000508004'; /* GB lang refset */


	drop temporary table if exists description_tmp;
	drop temporary table if exists tmp_pt;
	drop temporary table if exists tmp_edited_concept;
	
	commit;
	
	