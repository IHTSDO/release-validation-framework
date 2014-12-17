/******************************************************************************** 
	component-centric-snapshot-description-unique-terms

	Assertion:
	Active terms associated with active concepts are unique within hierarchy.

********************************************************************************/

/* 	a list of concepts and their semantic tags, for active concepts edited this release */
	drop table if exists tmp_hierarchy;
	create table if not exists tmp_hierarchy as
	select distinct a.conceptid, concat('(',substring_index(a.term, '(', -1)) as semantictag
	from curr_description_s a
		join curr_description_d  b
			on a.conceptid = b.conceptid
            and a.active = 1
			and a.typeid = '900000000000003001' /* fully specified name */
		join curr_concept_s c
			on 	c.id = b.conceptid
			and c.active = 1;
	commit;	 

/* 	a list of descriptions and their hierarchies */
	drop table if exists tmp_description;
	create table if not exists tmp_description
	select a.id, a.conceptid, a.term, b.semantictag as semantictag
	from curr_description_s a
	join tmp_hierarchy b
	on a.conceptid = b.conceptid
	and a.active = 1
	where a.typeid = '900000000000013009'; /* syn */
	commit;

/* 	violators to the results table */	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		a.term
	from tmp_description a
	join tmp_hierarchy b
	on a.conceptid = b.conceptid
	group by binary a.term, a.semantictag
	having count(a.term) > 1
	and count(a.semantictag) > 1;
	commit;
	
	drop table if exists tmp_hierarchy;
	drop table if exists tmp_description;
	
