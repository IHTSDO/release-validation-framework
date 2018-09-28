
/******************************************************************************** 
	file-centric-snapshot-description-synonym-tag

	Assertion:
	For each active FSN associated with active concept there is a synonym that has the same text.

********************************************************************************/
	
/* 	active concepts having descriptions edited in the current release cycle */
	create table if not exists tmp_consedited as
	select distinct conceptid 
	from curr_concept_s a
	join curr_description_d b
	on a.id = b.conceptid
	and a.active = 1;

/* 	for edited concepts, list all FSNs, with and without semantic tags */
	create table if not exists tmp_fsn
	(index idx_tmp_fsn_cid (conceptid), index idx_tmp_fsn_twt (termwithouttag))
	as select replace(a.term, concat('(',substring_index(a.term, '(', -1)), '') as termwithouttag,
	a.id , a.conceptid , a.term 
	from curr_description_s a
	join tmp_consedited b
	on a.conceptid = b.conceptid
	and a.active = 1
	where a.typeid ='900000000000003001' 
		and a.term not like '%(product)' 
  		and a.term not like '%(medicinal product)'
  		and a.term not like '%(medicinal product form)'
  		and a.term not like '%(clinical drug)'
  		and a.term not like '%(substance)'
  		and a.term not like '%(product name)'
  		and a.term not like '%(packaged clinical drug)'
  		and a.term not like '%(real clinical drug)'
  		and a.term not like '%(real medicinal product)'
  		and a.term not like '%(real packaged clinical drug)'
  		and a.term not like '%(supplier)';

/* all terms for edited concepts */
	create table if not exists tmp_allterms as
	select a.id , a.conceptid , a.term
	from curr_description_s a 
	join tmp_consedited b
	on a.conceptid = b.conceptid
	and a.active = 1
	where a.typeid !='900000000000003001';

/* select the concepts that have synonyms that match the FSNs without semantic tags */
	create table if not exists tmp_termsmatch
	(index idx_tmp_tm_cid (conceptid), index idx_tmp_tm_twt (termwithouttag))
	as select a.* 
	from tmp_fsn a
	join tmp_allterms b
	on a.conceptid = b.conceptid
	and a.termwithouttag = b.term;

/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat(a.conceptid, ' |', a.term, '|')
	from tmp_fsn a
	left join tmp_termsmatch b
	on a.conceptid = b.conceptid
	and a.termwithouttag = b.termwithouttag
	where b.conceptid is null
	and b.termwithouttag is null;


/* 	clean up */
	drop table if exists tmp_consedited;
	drop table if exists tmp_allterms;
	drop table if exists tmp_fsn;
	drop table if exists tmp_termsmatch;
	