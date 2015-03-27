
/******************************************************************************** 
	component-centric-snapshot-description-valid characters

	Assertion:
	Active Terms of active concept consist of valid characters.

********************************************************************************/
	
/* 	view of current snapshot made by finding all the active term for active concepts containing invalid character*/

/* http://ihtsdo.org/fileadmin/user_upload/doc/en_us/tig.html?t=trg2main_relatefiles */
	
	/* 	inserting exceptions in the result table for FSN*/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: FSN=',a.term, ':contains invalid character.') 	
	from  curr_description_d a , curr_concept_s b 
	where a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and a.typeid ='900000000000003001'
	and term REGEXP '[\\\t\r\n\Z\@$%#]';
	
	
	/* 	inserting exceptions in the result table for Synonym */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: Synonym=',a.term, ':contains invalid character.') 	
	from  curr_description_d a , curr_concept_s b 
	where a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and a.typeid ='900000000000013009'
	and term REGEXP '[\\\t\r\n\Z@$]';
	