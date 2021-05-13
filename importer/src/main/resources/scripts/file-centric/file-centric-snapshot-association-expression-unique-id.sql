
/******************************************************************************** 
	file-centric-snapshot-association-expression-unique-id.sql
	Assertion:
	ID is unique in the ASSOCIATION EXPRESION REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('id=',a.id, ':Non unique id in Association Expression Snapshot file.'),
		a.id,
        'curr_expressionassociationrefset_s'
	from curr_expressionassociationrefset_s a
	group by a.id
	having  count(a.id) > 1;
	