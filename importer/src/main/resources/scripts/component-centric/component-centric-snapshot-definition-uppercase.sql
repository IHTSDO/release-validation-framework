
/******************************************************************************** 
	component-centric-snapshot-definition-uppercase

	Assertion:
	The first letter of the Term should be capitalized in Text-Definition.

********************************************************************************/

/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		temp.conceptid,
		concat('TEXTDEFINITION: id=',temp.id, ' contains a term that the first letter is not capitalized.') 	
	from ( select SUBSTRING(a.term , 1, 1) as originalcase ,  UCASE(SUBSTRING(a.term , 1, 1)) as uppercase , a.id as id, a.conceptid as conceptid
	from curr_textdefinition_s a where a.active =1) as temp
	where BINARY temp.originalcase != temp.uppercase;
	commit;
	