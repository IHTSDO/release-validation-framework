/******************************************************************************** 
	component-centric-snapshot-description-casesignificanceid-def-proc

	Assertion:
	Defines procedure that tests that terms that contain EN-GB
	language-specific words are in the same GB language refset.

********************************************************************************/
	drop procedure if exists caseSignificance_procedure;
	create procedure caseSignificance_procedure(runid BIGINT, assertionid char(36)) 
	begin 
		declare no_more_rows INTEGER DEFAULT 0;
		declare thisTerm VARCHAR(255); 
		declare cs_term_cursor cursor for 
			select concat('"',SUBSTRING_INDEX(casesensitiveTerm, ' ', 1),'"') from res_casesensitiveTerm; 
		
		declare continue handler for not found set no_more_rows = 1;

		open cs_term_cursor; 

		validate: loop fetch cs_term_cursor into thisTerm; 

			if no_more_rows = 1 
				then close cs_term_cursor; 
				leave validate; 
			end if; 

			insert into qa_result (runid, assertion_id,concept_id, details)
			select 
				runid,
				assertionid,
				a.conceptid,
				concat('DESC: id=',a.id, ':Case-sensitive term containing ', thisTerm, ' has inappropriate caseSignificanceId.')
			from curr_description_d a , curr_concept_s b
			where a.casesignificanceid NOT IN (900000000000017005, 900000000000020002)
			and a.active = 1
			and b.active = 1
			and a.conceptid = b.id
			and MATCH a.term AGAINST (thisTerm IN BOOLEAN MODE)
			group by a.conceptid, a.id;
		end loop validate; 
	end;