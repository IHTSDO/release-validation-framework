
/******************************************************************************** 
	file-centric-snapshot-association-valid-targetcomponentid

	Assertion:
	TargetComponentId refers to valid concepts in the ASSOCIATION REFSET snapshot file.

********************************************************************************/
	
/* 	view of current snapshot made by finding invalid TargetComponentId */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.targetcomponentid
	from curr_associationrefset_s a
	left join curr_concept_s b
	on a.targetcomponentid = b.id
	where b.id is null;
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOC RS: Targetcomponentid=',a.targetcomponentid, ':Invalid TargetComponentId.') 	
	from v_curr_snapshot a;

	drop table if exists v_curr_snapshot;
