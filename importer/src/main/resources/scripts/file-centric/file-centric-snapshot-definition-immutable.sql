
/******************************************************************************** 
	file-centric-snapshot-definition-immutable

	Assertion:
	There is a 1:1 relationship between the ID and the immutable values in DEFINITION snapshot.

********************************************************************************/

/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DEFINITION: id=',a.id, ' is referneced with more than one set of immutable values in the definition snapshot.') 	
	from curr_textdefinition_s a 
	group by a.id , a.typeid , a.languagecode , a.conceptid
	having count(a.id) > 1 and count(a.typeid ) > 1 and count(languagecode) > 1 and count(conceptid) > 1;
	