
/******************************************************************************** 
	component-centric-snapshot-description-casesignificanceid

	Assertion:
	Case-sensitive terms have appropriate caseSignificanceId.

********************************************************************************/
	
/* 	view of current snapshot made by finding all the noncasesitive term for active concepts */
	
	drop table if exists t_curr_snapshot_1;
	drop table if exists t_curr_snapshot_2;

	
	create table t_curr_snapshot_1 as
	select a.*
	from  curr_description_d a , curr_concept_s b 
	where a.casesignificanceid != 900000000000017005
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id;

	create index idx_term on t_curr_snapshot_1(term);
	
	create table t_curr_snapshot_2 as
	select a.id
	from  t_curr_snapshot_1 a , res_casesensitiveTerm b
	where a.term like b.casesensitiveTerm  ;  
	
	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: id=',a.id, ':Case-sensitivity terms containing inappropriate caseSignificanceId.') 	
	from t_curr_snapshot_2 a;


	drop table t_curr_snapshot_1;
	drop table t_curr_snapshot_2;

	