
/******************************************************************************** 
	file-centric-snapshot-definition-valid conceptid

	Assertion:
	ConceptId value refers to valid concept identifier in DEFINITION snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding invalid conceptid values  */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id
	from curr_textdefinition_s a
	left join curr_concept_s b
	on a.conceptid = b.id
	where b.id is null;
	

	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DEFINITION: id=',a.id, ' refers to an invalid concept id in the Text Definition snapshot.') 	
	from v_curr_snapshot a;
	drop table if exists v_curr_snapshot;
