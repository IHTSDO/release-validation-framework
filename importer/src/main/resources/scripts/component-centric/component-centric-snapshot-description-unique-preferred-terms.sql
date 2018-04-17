/******************************************************************************** 
	component-centric-snapshot-description-unique-preferred-terms

	Assertion:
	Active preferred terms for active concepts are unique in the same hierarchy.

********************************************************************************/

	drop table if exists temp_active_fsn_hierarchy;
	create table if not exists temp_active_fsn_hierarchy as
	select distinct a.conceptid, a.languagecode, concat('(',substring_index(a.term, '(', -1)) as semantictag
	from curr_description_s a
	join curr_concept_s b
		on a.conceptid = b.id
	where a.active = 1
		and b.active =1
		and a.typeid = '900000000000003001'; /* fully specified name */
		
	alter table temp_active_fsn_hierarchy add index idx_tmp_afh_cid (conceptId);
	alter table temp_active_fsn_hierarchy add index idx_tmp_afh_l (languagecode);
	alter table temp_active_fsn_hierarchy add index idx_tmp_afh_st (semantictag);

/* 	a list of descriptions and their hierarchies */
	drop table if exists tmp_description_syn;
	create table if not exists tmp_description_syn as 
	select a.id, a.languagecode, a.conceptid, a.term, b.semantictag as semantictag
	from curr_description_s a
	join temp_active_fsn_hierarchy b
		on a.conceptid = b.conceptid
		and a.languagecode = b.languagecode
	where a.active =1
	and a.typeid = '900000000000013009'
	and exists (select id from curr_langrefset_s c where c.referencedcomponentid=a.id and c.acceptabilityid='900000000000548007' and c.active=1);

	alter table tmp_description_syn add index idx_tmp_ds_cid (conceptId);
	alter table tmp_description_syn add index idx_tmp_ds_l (languagecode);
	alter table tmp_description_syn add index idx_tmp_ds_st (semantictag);
	alter table tmp_description_syn add index idx_tmp_ds_t (term);

/* 	violators to the results table */	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Preferred term=', a.term, ' is duplicated in hierarchy ', a.semantictag)
	from tmp_description_syn a,
	(select a.term, a.semantictag from tmp_description_syn a 
		group by a.term, a.semantictag
		having count(a.id) > 1) as duplicate
	where a.term = duplicate.term
		and a.semantictag = duplicate.semantictag;
	