
/******************************************************************************** 
	file-centric-snapshot-association-unique-id

	Assertion:
	ID is unique in the ASSOCIATION REFSET snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('ASSOC RS: id=',a.id, ':Non unique id in current release file.'),
		a.id,
		'curr_associationrefset_s'
	from curr_associationrefset_s a
	group by a.id
	having  count(a.id) > 1;
	commit;
