
/******************************************************************************** 
	component-centric-snapshot-description-consistent-semantic-tag

	Assertion:
	All active FSNs for a given concept have the same semantic tag.

********************************************************************************/
	
/* 	view of current snapshot made by finding active FSN's and semantic tags */
	create or replace view v_curr_snapshot_1 as
	select  a.conceptid , a.id , SUBSTRING(term,LOCATE('(',term)) as tag , a.term 
	from curr_description_s a 
	where a.typeid ='900000000000003001'
	and a.active = 1
	and a.term like '% (%)';

	create or replace view v_curr_snapshot_2 as
	SELECT a.conceptid
	from curr_description_s a , v_curr_snapshot_1 b
	where a.typeid in ('900000000000003001')
	and a.conceptid = b.conceptid
	and b.tag !=  SUBSTRING(a.term, LOCATE('(', a.term))
	and a.active = 1;


	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: conceptid=',a.conceptid, ':Active FSN for a given concept with different semantic tag.') 	
	from v_curr_snapshot_2 a;


	drop view v_curr_snapshot_1;
	drop view v_curr_snapshot_2;

