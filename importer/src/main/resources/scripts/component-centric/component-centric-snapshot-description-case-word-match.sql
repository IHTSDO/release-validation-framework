
/******************************************************************************** 
	component-centric-snapshot-description-case-word-match

	Assertion:
	Active case-sensitive terms of active concepts that share initial words also 
	share caseSignificanceId value.

	note: 	implementation is limited to terms associated with concepts that have 
			descriptions edited for the current prospective release. This is to 
			reduce noise related to violations in prior releases.  

********************************************************************************/
	
/*  view of active concepts associated with descriptions that have been edited 
	for the currently prospective release
*/
	create or replace view tmp_active_con as
	select a.*
	from curr_concept_s a
		join curr_description_d b
			on a.id = b.conceptid
			and a.active = 1 
			and b.active = 1;
	commit;

/* 	view of current snapshot made by finding all the casesitive term for the 
	above concepts 
*/
	create or replace view v_curr_snapshot_1 as
	select SUBSTRING_INDEX(term, ' ', 1) as firstword , a.conceptid , a.id , a.term , a.casesignificanceid
	from  curr_description_s a , tmp_active_con b
	where a.casesignificanceid = 900000000000017005
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id;
	
	create or replace view v_curr_snapshot_2 as
	select a.conceptid , a.term ,  a.casesignificanceid 
	from v_curr_snapshot_1 a , curr_description_s b , tmp_active_con c
	where b.casesignificanceid = 900000000000020002
	and b.active = 1
	and c.active = 1
	and b.conceptid = c.id
	and a.conceptid = b.conceptid
	and BINARY SUBSTRING_INDEX(b.term, ' ', 1) = firstword;	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: conceptid=',a.conceptid, ':Terms not sharing case-sensitivity.') 	
	from v_curr_snapshot_2 a;


	drop view v_curr_snapshot_1;
	drop view v_curr_snapshot_2;

	