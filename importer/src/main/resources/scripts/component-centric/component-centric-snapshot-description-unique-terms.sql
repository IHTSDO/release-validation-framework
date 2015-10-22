/******************************************************************************** 
	component-centric-snapshot-description-unique-terms

	Assertion:
	Active terms associated with active concepts are unique within hierarchy.

********************************************************************************/

drop table if exists temp_active_fsn_hierarchy;
	create table if not exists temp_active_fsn_hierarchy as
	select distinct a.conceptid, a.languagecode, concat('(',substring_index(a.term, '(', -1)) as semantictag
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
	drop table if exists tmp_description_syn;
	create table if not exists tmp_description_syn
	select a.id, a.languagecode, a.conceptid, a.term, b.semantictag as semantictag
	from curr_description_s a
	join temp_active_fsn_hierarchy b
	on a.conceptid = b.conceptid
	and a.active = 1
	and a.languagecode = b.languagecode
	where a.typeid = '900000000000013009'; /* syn */
	commit;

/* 	violators to the results table */	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		a.term
	from tmp_description_syn a
	join temp_active_fsn_hierarchy b
	on a.conceptid = b.conceptid
	and a.languagecode = b.languagecode
	group by binary a.term, a.semantictag, a.conceptid
	having count(a.term) > 1
	and count(a.semantictag) > 1;
	commit;
	
	drop table if exists temp_active_fsn_hierarchy;
	drop table if exists tmp_description_syn;
	
