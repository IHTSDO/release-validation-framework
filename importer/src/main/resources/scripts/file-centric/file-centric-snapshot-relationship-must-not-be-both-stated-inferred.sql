/********************************************************************************
	file-centric-snapshot-relationship-must-not-be-both-stated-inferred.sql

	Assertion:
	Relationships should not exist in Stated relationship and Inferred relationship files at the same time

********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		sourceid,
		concat('RELATIONSHIP : id=',id, ': Relationship Id exists in both Relationship and Stated Relationship files')
	from curr_relationship_s a
	where a.id in (select id from curr_stated_relationship_s);
