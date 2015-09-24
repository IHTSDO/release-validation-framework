
/******************************************************************************** 
	file-centric-snapshot-description-immutable

	Assertion:
	There is a 1:1 relationship between the id and the immutable values in DESCRIPTION snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: id=',a.id, ':There is a 1:1 relationship between the id and the immutable values in DESCRIPTION snapshot.') 	
	from curr_description_s a 
	group by a.id , a.typeid , a.languagecode , a.conceptid
	having count(a.id) > 1 and count(a.typeid ) > 1 and count(languagecode) > 1 and count(conceptid) > 1;