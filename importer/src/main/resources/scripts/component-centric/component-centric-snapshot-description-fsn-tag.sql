
/******************************************************************************** 
	file-centric-snapshot-description-fsn-tag

	Assertion:
	All active FSNs associated with active concept have a semantic tag.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's without semantic tags */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.term
	from curr_description_s a , curr_concept_s b
	where a.typeid ='900000000000003001'
	and a.active = 1
	and a.term not like '% (%)'
	and a.conceptid = b.id
	and b.active = 1;

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: Term=',a.term, ':Fully Specified Name without semantic tag.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;

	