/******************************************************************************** 
	component-centric-delta-language-lang-specific-words-gb-def-proc

	Assertion:
	Defines procedure that tests that terms that contain EN-GB
	language-specific words are in the same GB language refset.

********************************************************************************/
	
	drop procedure if exists gbterm_procedure;
	create procedure gbterm_procedure(runid int, assertionuuid char(36), assertiontext varchar(255)) 
	begin 
		declare no_more_rows boolean default false; 
		declare gbTerm VARCHAR(255); 
		declare term_cursor cursor for 
			select term from res_gbterm; 

		declare continue handler for not found set no_more_rows := true; 

		open term_cursor; 

		LOOP1: 
			loop fetch term_cursor into gbTerm; 

			if no_more_rows 
				then close term_cursor; 
				leave LOOP1; 
			end if; 

			insert into qa_result (runid, assertionuuid, assertiontext, details)
			select 
				runid,
				assertionuuid,
				assertiontext,
				concat('DESCRIPTION: id=',a.id, ': Synonym is in GB Language refset and refers to a term that is in en-GB spelling.') 
			from v_curr_delta a 	
			where locate(gbTerm, a.term) >= 1;
		end loop LOOP1; 
	end