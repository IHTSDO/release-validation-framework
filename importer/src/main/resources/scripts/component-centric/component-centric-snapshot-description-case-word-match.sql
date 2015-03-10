
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
	drop table if exists v_tmp_active_con;
	create table if not exists v_tmp_active_con as
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
	drop table if exists v_curr_snapshot_1;
	create table if not exists v_curr_snapshot_1 as
	select SUBSTRING_INDEX(term, ' ', 1) as firstword , a.conceptid , a.id , a.term , a.casesignificanceid, a.effectivetime
	from  curr_description_s a , v_tmp_active_con b
	where a.casesignificanceid = 900000000000017005
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id;
	
	drop table if exists v_curr_snapshot_2;
	create table if not exists v_curr_snapshot_2 (INDEX(conceptid)) as
	select distinct (a.conceptid)
	from v_curr_snapshot_1 a , curr_description_s b , v_tmp_active_con c
	where b.casesignificanceid = 900000000000020002
	and b.active = 1
	and c.active = 1
	and b.conceptid = c.id
	and a.conceptid = b.conceptid
	and cast(b.effectivetime as datetime) >= cast(a.effectivetime as datetime) 
	and BINARY SUBSTRING_INDEX(b.term, ' ', 1) = firstword;	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: conceptid=',a.conceptid, ':has terms not sharing case-sensitivity.') 	
	from v_curr_snapshot_2 a;


	drop table if exists v_curr_snapshot_1;
	drop table if exists v_curr_snapshot_2;

	