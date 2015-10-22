
/******************************************************************************** 
	file-centric-snapshot-description-closing-parens

	Assertion:
	All Active FSNs associated with active concepts ends in closing parentheses.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: Term=',a.term, ':Fully Specified Name not ending with closing parantheses.') 	
	from curr_description_s a , curr_concept_s b
	where a.typeid = '900000000000003001'
	and a.conceptid = b.id
	and b.active =1 
	and a.active = 1
	and a.term not like '%)';
	commit;


	