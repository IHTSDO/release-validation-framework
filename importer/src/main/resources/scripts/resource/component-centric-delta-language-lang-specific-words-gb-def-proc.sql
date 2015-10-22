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
			select term from res_usterm; 

		declare continue handler for not found set no_more_rows = 1;

		open term_cursor; 

		validate: loop fetch term_cursor into usTerm; 

			if no_more_rows = 1 
				then close term_cursor; 
				leave validate; 
			end if; 

			insert into qa_result (runid, assertion_id, details)
			select 
				runid,
				assertionid,
				concat('DESCRIPTION: id=',a.id, ': Synonym is prefered in the GB Language refset but refers to a word has en-us spelling: ',usTerm) 
			from v_curr_delta_gb a 	
			where a.term REGEXP  concat('[[:<:]]', usTerm, '[[:>:]]');
		end loop validate; 
	end;