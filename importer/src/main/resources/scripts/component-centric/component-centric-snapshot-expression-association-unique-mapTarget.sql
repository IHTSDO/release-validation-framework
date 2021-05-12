
/******************************************************************************** 
component-centric-snapshot-expression-association-unique-mapTarget.sql

	Assertion:
	The mapTarget in expression association refset snapshot is unique.

********************************************************************************/
insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
 select
 	<RUNID>,
	'<ASSERTIONUUID>',
 	a.referencedcomponentid,
 	concat('MapTarget:',a.mapTarget,' is not unique in the expression association refset snapshot.'),
 	null,
 	null
 from curr_expressionassociationrefset_s a
	group by a.mapTarget
	having count(a.mapTarget) > 1;
 commit;
 