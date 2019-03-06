/********************************************************************************
	file-centric-snapshot-relationship-must-not-be-both-stated-inferred.sql

	Assertion:
	Relationships should not exist in Stated relationship and Inferred relationship files at the same time

********************************************************************************/

/*
Get the list of modules in this package
 */
drop table if exists tmp_modules_list;
create table if not exists tmp_modules_list
as select m.moduleid from curr_moduledependencyrefset_d m
where m.active = 1 and m.referencedcomponentid = '900000000000012004';

insert into qa_result (runid, assertionuuid, concept_id, details)
	select
		<RUNID>,
		'<ASSERTIONUUID>',
		sourceid,
		concat('RELATIONSHIP : id=',id, ': Relationship Id exists in both Relationship and Stated Relationship files')
	from curr_relationship_s a
	where a.moduleid in (select moduleid from tmp_modules_list)
	and a.id in (select id from curr_stated_relationship_s b where b.moduleid in (select moduleid from tmp_modules_list));

drop table if exists tmp_modules_list;
