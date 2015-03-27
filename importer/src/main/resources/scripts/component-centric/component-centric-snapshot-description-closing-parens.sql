
/******************************************************************************** 
	file-centric-snapshot-description-closing-parens

	Assertion:
	All Active FSNs associated with active concepts ends in closing parentheses.

********************************************************************************/
	
/* 	view of current snapshot made by finding FSN's not ending with closing parantheses */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.term 
	from curr_description_s a , curr_concept_s b
	where a.typeid = '900000000000003001'
	and a.conceptid = b.id
	and b.active =1 
	and a.active = 1
	and a.term not like '%)';


	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: Term=',a.term, ':Fully Specified Name not ending with closing parantheses.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;

	