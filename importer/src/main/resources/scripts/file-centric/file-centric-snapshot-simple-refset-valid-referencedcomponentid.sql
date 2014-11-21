
/******************************************************************************** 
	file-centric-snapshot-association-valid-referencedcomponentid

	Assertion:
	Referencedcomponentid refers to valid concepts in the ASSOCIATION REFSET snapshot file.

********************************************************************************/
	drop table if exists t_curr_snapshot;
	
/* 	view of current snapshot made by finding invalid referencedcomponentid */
	create table t_curr_snapshot as
	select a.referencedcomponentid
	from curr_associationrefset_s a
	left join curr_concept_s b
	on a.referencedcomponentid = b.id
	where b.id is null;
	
  
	delete from t_curr_snapshot 
	where referencedcomponentid in (
	select id from curr_description_s);
	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('Simple RS: id=',a.referencedcomponentid, ':Invalid Referencedcomponentid in ASSOCIATION REFSET snapshot.') 	
	from t_curr_snapshot a;


	drop table if exists t_curr_snapshot;
