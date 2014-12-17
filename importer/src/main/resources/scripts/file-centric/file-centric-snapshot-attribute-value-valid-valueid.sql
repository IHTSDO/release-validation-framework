
/******************************************************************************** 
	file-centric-snapshot-attribute-value-valid-valueid

	Assertion:
	ValueId refers to valid concepts in the ATTRIBUTE VALUE snapshot.

********************************************************************************/
	
/* 	view of current snapshot made by finding invalid valueid identifiers */
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.valueid
	from curr_attributevaluerefset_s a
	left join curr_concept_s b
	on a.valueid = b.id
	where b.id is null;
		
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ATT RF: id=',a.valueid, ':Invalid valueId.') 	
	from v_curr_snapshot a;




	drop table if exists v_curr_snapshot;
