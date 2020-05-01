/********************************************************************************
	component-centric-snapshot-mrcm-refset-validate-concept-is-valid-descendant-proc.sql

	Defines a procedure to validate whether a concept is a descendant of specified concepts in MRCM Refsets Snapshot

********************************************************************************/
drop procedure if exists validateConceptIdIsValidDescendant_proc;
create procedure validateConceptIdIsValidDescendant_proc(runId bigint, assertionId varchar(36), tableName varchar(255), columnName varchar(255), refsetName varchar(255), rootConceptIds varchar(1024), expression varchar(4000))
begin
declare currentDepth integer default 0;
declare parentsCount integer;

drop table if exists temp_snapshot_concept_hierachy_tree;
create table temp_snapshot_concept_hierachy_tree(
conceptId bigint(20) not null,
  parentId bigint(20) not null,
  depth integer
);

set @runSql = concat("insert into temp_snapshot_concept_hierachy_tree(conceptId, parentId, depth)
select sourceId, destinationId,", currentDepth ," from relationship_s s
where s.active = 1 and s.typeid = 116680003 and s.destinationId in (",rootConceptIds,");");

prepare statement from @runSql;
execute statement;
set parentsCount = (select count(distinct conceptId) from temp_snapshot_concept_hierachy_tree where depth = currentDepth);

while parentsCount > 0 do
insert into temp_snapshot_concept_hierachy_tree(conceptId, parentId, depth)
select sourceId, destinationId, (currentDepth + 1) from relationship_s s
where s.active = 1 and s.typeid = 116680003 and s.destinationId in (select distinct conceptId from temp_snapshot_concept_hierachy_tree where depth = currentDepth);
set parentsCount = (select count(distinct conceptId) from temp_snapshot_concept_hierachy_tree where depth = currentDepth);
set currentDepth = currentDepth + 1;
end while;

drop table if exists temp_snapshot_refset_conceptId;
create table temp_snapshot_refset_conceptId(id varchar(36), conceptId bigint(20));
set @runSql = concat("insert into temp_snapshot_refset_conceptId(id, conceptId) select id,", columnName, " from ", tableName, " where active = 1;");
prepare statement from @runSql;
execute statement;

insert into qa_result (run_id, assertion_id,concept_id, details)
select
	runId,
	assertionId,
	result.conceptId,
	concat(refsetName,":id=",result.id,":ConceptId=",result.conceptId, " referenced in the column ", columnName ," in SNAPSHOT is not valid descendant of expression ", expression)
	from  (select id, conceptId from temp_snapshot_refset_conceptId where conceptId not in (select conceptId from temp_snapshot_concept_hierachy_tree)) as result;

drop table if exists temp_snapshot_concept_hierachy_tree;
drop table if exists temp_snapshot_refset_conceptId;
end;