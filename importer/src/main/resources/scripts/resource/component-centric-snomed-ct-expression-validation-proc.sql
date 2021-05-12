/******************************************************************************** 
	component-centric-snomed-ct-expression-validation-proc.sql

	Defines a procedure to test the validity of the concept ids in the SNOMED CT expression for LOIC

********************************************************************************/
DROP PROCEDURE if exists validateSnomedCTExpressionConcepts_procedure;
CREATE PROCEDURE validateSnomedCTExpressionConcepts_procedure(runId BIGINT, assertionId varchar(36))
begin 
		declare no_more_rows INTEGER DEFAULT 0;
		declare ids VARCHAR(500); 
		declare expression_cursor cursor for 
			select expression from expressionassociationrefset_s;

		declare continue handler for not found set no_more_rows = 1;
		drop table if exists temp_concept;
		CREATE TABLE temp_concept (conceptId VARCHAR(30));

		open expression_cursor; 

		validate: loop fetch expression_cursor into ids; 
			if no_more_rows = 1 
				then close expression_cursor; 
				leave validate; 
			end if; 
	
		set @sqlStr = CONCAT("INSERT INTO temp_concept (conceptId) VALUES ('",REPLACE((SELECT GROUP_CONCAT(cleanExpression(ids))), ",", "'),('"),"');");
		PREPARE statement FROM @sqlStr;
		execute statement;
		end loop validate; 
		insert into qa_result (run_id, assertion_id,concept_id, details, component_id, table_name)
			select 
				runId,
				assertionId,
				result.conceptId,
				concat('Concept: id=',result.conceptId, ' referenced in the ExpressionAssociationRefset SNAPSHOT is unknown.'),
				null,
				'curr_expressionassociationrefset_s'
			from  (select distinct(conceptId) from temp_concept a left join curr_concept_s b on a.conceptId = b.id where b.id is null) as result;
		drop table temp_concept;
end;