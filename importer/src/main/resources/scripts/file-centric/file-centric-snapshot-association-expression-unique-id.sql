
/******************************************************************************** 
	file-centric-snapshot-association-expression-unique-id.sql
	Assertion:
	ID is unique in the ASSOCIATION EXPRESION REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('id=',a.id, ':Non unique id in Association Expression Snapshot file.') 	
	from curr_expressionAssociationRefset_s a
	group by a.id
	having  count(a.id) > 1;
	