/******************************************************************************** 
	component-centric-snapshot-description-unique-terms

	Assertion:
	No active synonyms associated with active concepts have semantic tags.

********************************************************************************/

/* 	a list of concepts and their semantic tags, for active concepts edited this release */
	drop table if exists tmp_hierarchy;
	create table if not exists tmp_hierarchy as
	select distinct concat('(',substring_index(a.term, '(', -1)) as semantictag
	from curr_description_s a
		join res_concepts_edited b
			on a.conceptid = b.conceptid
		join curr_concept_s c
			on 	c.id = b.conceptid
			and c.active = 1
	where a.typeid = '900000000000003001' /* fully specified name */
	and a.active = 1;
	commit;	 

/* 	a list of descriptions and their hierarchies */
	drop table if exists tmp_description;
	create table if not exists tmp_description
	select a.conceptid, a.term, concat('(',substring_index(a.term, '(', -1)) as semantictag
	from curr_description_s a
		join res_concepts_edited b
			on a.conceptid = b.conceptid
		join curr_concept_s c
			on 	c.id = b.conceptid
			and c.active = 1
	where
	a.term like '%(%)%'
	and a.typeid = '900000000000013009'  /* synonym */
	and a.active = 1
	having semantictag in ( select semantictag from tmp_hierarchy);
	commit;

/* 	violators are the ones where the term contains the semantic tag */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Concept: id=',a.conceptid, ' has synonym containing a semantic tag ',a.semantictag)  
	from tmp_description a;
	commit;
	
	drop table if exists tmp_hierarchy;
	drop table if exists tmp_description;
