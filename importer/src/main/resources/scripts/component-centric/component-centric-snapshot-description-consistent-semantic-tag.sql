
/******************************************************************************** 
	component-centric-snapshot-description-consistent-semantic-tag

	Assertion:
	All active FSNs for a given concept have the same semantic tag.

********************************************************************************/
	
/* 	view of current snapshot made by finding active FSN's and semantic tags */
	drop table if exists tmp_active_fsn;
	create table if not exists tmp_active_fsn (INDEX(conceptid)) as
	select  a.conceptid , a.id , SUBSTRING(term,LOCATE('(',term)) as tag , a.term, a.languagecode 
	from curr_description_s a 
	where a.typeid ='900000000000003001'
	and a.active = 1
	and a.term like '% (%)';

	drop table if exists tmp_different_tag;
	create table if not exists tmp_different_tag (INDEX(conceptid)) as
	SELECT a.conceptid
	from curr_description_s a , tmp_active_fsn b
	where a.typeid in ('900000000000003001')
	and a.conceptid = b.conceptid
	and a.languagecode = b.languagecode 
	and b.tag !=  SUBSTRING(a.term, LOCATE('(', a.term))
	and a.active = 1;


	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESC: conceptid=',a.conceptid, ':Active FSN for a given concept with different semantic tag.') 	
	from tmp_different_tag a;

	drop table if exists tmp_active_fsn;
	drop table if exists tmp_different_tag;

