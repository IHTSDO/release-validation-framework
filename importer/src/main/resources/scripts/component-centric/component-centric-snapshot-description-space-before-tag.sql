
/******************************************************************************** 
	file-centric-snapshot-description-space-before-tag

	Assertion:
	All active FSNs associated with active concepts have a space before the semantic tag.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's semantic tag */
	create or replace view v_curr_snapshot as
	SELECT a.term
	from curr_description_s a , curr_concept_s b
	where typeid ='900000000000003001'
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and a.term not like '%)' 
	and a.term not like '% (%';
	
	
/* inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: term=',a.term, ':Fully Specified Name has no space before the semantic tag.')	
	from v_curr_snapshot a
	where a.term not like ' (%';


	drop view v_curr_snapshot;

	