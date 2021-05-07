
/******************************************************************************** 
	file-centric-snapshot-mrcm-never-grouped-relationship-grouped.sql

	Assertion:
	Found grouped relationship for attribute marked as never-grouped

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		r.sourceid,
		concat('Relationship groups a never-grouped relationship type: id=',r.id,' has an invalid value ', r. relationshipgroup, ' for the relationship group field.')
	from curr_mrcmAttributeDomainRefset_s a, curr_relationship_s r
	where a.grouped = '0' and a.referencedcomponentid = r. typeid and r. relationshipgroup > 0;
	commit;