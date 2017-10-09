/******************************************************************************** 
	component-centric-delta-language-lang-specific-words-gb-def-proc

	Assertion:
	Defines procedure that tests that terms that contain EN-GB
	language-specific words are in the same GB language refset.

********************************************************************************/
	drop procedure if exists gbTerm_procedure;
	create procedure gbTerm_procedure(runid BIGINT, assertionid char(36)) 
	begin 
		declare no_more_rows INTEGER DEFAULT 0;
		declare usTerm VARCHAR(255); 
		declare term_cursor cursor for 
			select distinct us_term from res_us_gb_terms; 

		declare continue handler for not found set no_more_rows = 1;

		open term_cursor; 

		validate: loop fetch term_cursor into usTerm; 

			if no_more_rows = 1 
				then close term_cursor; 
				leave validate; 
			end if; 

			insert into qa_result (runid, assertion_id,concept_id, details)
			select 
				runid,
				assertionid,
				a.conceptid,
				concat('DESCRIPTION: id=',a.id, ': Synonym is preferred in the en-gb language refset but refers to a word that has en-us spelling: ',usTerm) 
			from v_curr_delta_gb a 	
			where MATCH a.term AGAINST (usTerm IN BOOLEAN MODE);
		end loop validate; 
	end;