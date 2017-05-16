/********************************************************************************
	component-centric-snapshot-mrcm-attribute-range-refset-validate-concept-is-valid-descendant-proc.sql

	Defines a procedure to test the validity of the concept ids in the SNOMED CT expression in MRCM Attribute Range Refsets

********************************************************************************/
drop procedure if exists validateConceptIdsInMRCMAttributeRangeRefsetExpression_procedure;
create procedure validateConceptIdsInMRCMAttributeRangeRefsetExpression_procedure(runId BIGINT, assertionId varchar(36), tableName varchar(255), columnName varchar(255), refsetName varchar(255))
begin
declare no_more_rows integer default 0;
declare populateConceptIdSql text;
declare sql_cursor cursor for select insertSql from temp_attr_range_refset_expression_concept_query;
declare continue handler for not found set no_more_rows = 1;

drop table if exists temp_attr_range_refset_expression_concept_id;
create table temp_attr_range_refset_expression_concept_id(id varchar(36), conceptId bigint(20));

drop table if exists temp_attr_range_refset_expression_split;
create table temp_attr_range_refset_expression_split(id varchar(36), split varchar(255));

drop table if exists temp_attr_range_refset_expression_concept_query;
create table temp_attr_range_refset_expression_concept_query(id varchar(36), insertSql text);


set @copyExpressionSql = concat("insert into temp_attr_range_refset_expression_concept_query(id, insertSql) select id,", columnName, " from ", tableName);
prepare statement from @copyExpressionSql;
execute statement;

update temp_attr_range_refset_expression_concept_query set insertSql = concat("insert into temp_attr_range_refset_expression_split(split,id) values ('",replace((trim(insertSql))," ", concat("','",id,"'),('")),"','",id,"');");


open sql_cursor;
executeQueries: loop fetch sql_cursor into populateConceptIdSql;
if no_more_rows = 1
then close sql_cursor;
leave executeQueries;
end if;

set @runSql = populateConceptIdSql;
prepare statement from @runSql;
execute statement;

insert into temp_attr_range_refset_expression_concept_id(id, conceptId) select id, CAST(split as unsigned) from temp_attr_range_refset_expression_split where split REGEXP '^[0-9]{6,20}$';

end loop executeQueries;

insert into qa_result (run_id, assertion_id,concept_id, details)
select
	runId,
	assertionId,
	result.conceptId,
	concat(refsetName,":id=",result.id,":ConceptId=",result.conceptId, " referenced in the column ", columnName ," in SNAPSHOT is invalid.")
	from  (select distinct(a.conceptId), a.id from temp_attr_range_refset_expression_concept_id a left join
    curr_concept_s b
    on a.conceptId = b.id where b.id is null or b.id = 0 group by a.id) as result;


drop table if exists temp_attr_range_refset_expression_concept_id;
drop table if exists temp_attr_range_refset_expression_split;
drop table if exists temp_attr_range_refset_expression_concept_query;
end;
