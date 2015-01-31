
/******************************************************************************** 
	file-centric-snapshot-description-valid-conceptid

	Assertion:
	ConceptId value refers to valid concept identifier in DESCRIPTION snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by valid identifiers */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.conceptid
	from curr_description_s a
	left join curr_concept_s b
	on a.conceptid = b.id
	where b.id is null;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TEXTDEF: id=',a.conceptid, ':ConceptId value refers to valid concept identifier in DESCRIPTION snapshot.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
