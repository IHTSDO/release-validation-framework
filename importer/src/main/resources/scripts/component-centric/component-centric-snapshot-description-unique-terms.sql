/******************************************************************************** 
	component-centric-snapshot-description-unique-terms

	Assertion:
	Active terms associated with active concepts are unique within hierarchy.

********************************************************************************/

	drop table if exists temp_active_fsn_hierarchy;
	create table if not exists temp_active_fsn_hierarchy as
	select distinct a.conceptid, a.languagecode, concat('(',substring_index(a.term, '(', -1)) as semantictag
	from curr_description_s a
	join curr_concept_s b
		on a.conceptid = b.id
	where a.active = 1
		and b.active =1
		and a.moduleid != '715515008'
		and a.typeid = '900000000000003001'; /* fully specified name */
		
	alter table temp_active_fsn_hierarchy add index idx_tmp_afh_cid (conceptId);
	alter table temp_active_fsn_hierarchy add index idx_tmp_afh_l (languagecode);
	alter table temp_active_fsn_hierarchy add index idx_tmp_afh_st (semantictag);

/* 	a list of descriptions and their hierarchies */
	drop table if exists tmp_description_syn;
	create table if not exists tmp_description_syn (
	id bigint(20) not null,
	languagecode varchar(2) not null,
	conceptid bigint(20) not null,
	term varchar(256) not null,
	semantictag varchar(100));
	
	insert into tmp_description_syn (id, languagecode, conceptid, term, semantictag) 
	select a.id, a.languagecode, a.conceptid, a.term, b.semantictag as semantictag
	from curr_description_s a
	join temp_active_fsn_hierarchy b
		on a.conceptid = b.conceptid
		and a.languagecode = b.languagecode
	where a.moduleid != '715515008'
	and a.active =1
	and a.typeid = '900000000000013009'; /* syn */
	
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
		concat('Synonym=', a.term, ' is duplicated within hierarchy ', a.semantictag)
	from tmp_description_syn a,
	(select a.term from tmp_description_syn a 
		group by a.term, a.semantictag
		having count(a.id) > 1) as duplicate
	where a.term = duplicate.term;
	