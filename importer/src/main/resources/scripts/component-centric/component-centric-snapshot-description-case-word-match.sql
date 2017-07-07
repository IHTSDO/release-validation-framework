
/******************************************************************************** 
	component-centric-snapshot-description-case-word-match

	Assertion:
	Active case-sensitive terms of active concepts that share initial words also 
	share caseSignificanceId value.

	note: 	implementation is limited to terms associated with concepts that have 
			descriptions edited for the current prospective release. This is to 
			reduce noise related to violations in prior releases.  

********************************************************************************/
	
/* 	view of current snapshot made by finding all the casesitive term for the 
	above concepts 
*/
	drop table if exists tmp_active_caseSensitive_description;
	create table if not exists tmp_active_caseSensitive_description as
	select SUBSTRING_INDEX(term, ' ', 1) as firstword , a.conceptid , a.id , a.term , a.casesignificanceid, a.effectivetime, a.languagecode
	from  curr_description_s a , res_edited_active_concepts b
	where a.casesignificanceid = 900000000000017005
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id;
	
	drop table if exists tmp_caseSignificanceId_not_match;
	create table if not exists tmp_caseSignificanceId_not_match (INDEX(conceptid)) as
	select distinct (a.conceptid)
	from tmp_active_caseSensitive_description a , curr_description_s b , res_edited_active_concepts c
	where b.casesignificanceid = 900000000000020002
	and b.active = 1
	and c.active = 1
	and b.conceptid = c.id
	and a.conceptid = b.conceptid
	and a.languagecode = b.languagecode
	and cast(b.effectivetime as datetime) >= cast(a.effectivetime as datetime) 
	and BINARY SUBSTRING_INDEX(b.term, ' ', 1) = firstword;	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESC: conceptid=',a.conceptid, ':has terms not sharing case-sensitivity.') 	
	from tmp_caseSignificanceId_not_match a;


	drop table if exists tmp_active_caseSensitive_description;
	drop table if exists tmp_caseSignificanceId_not_match;

	