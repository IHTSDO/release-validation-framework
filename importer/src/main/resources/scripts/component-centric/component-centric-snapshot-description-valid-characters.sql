
/******************************************************************************** 
	component-centric-snapshot-description-valid-characters

	Assertion:
	Active Terms of active concept consist of valid characters.

********************************************************************************/
	
/* 	view of current snapshot made by finding all the active term for active concepts containing invalid character*/

/* www.snomed.org/tig?t=terms_SpecialCharacters */
	
	/* 	inserting exceptions in the result table for FSN*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION ID=',a.id, ': FSN=',a.term, ' contains invalid character.') 	
	from  curr_description_d a , curr_concept_s b 
	where a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and a.typeid ='900000000000003001'
	and term REGEXP '[\\\t\r\n\Z\@$#]';
	
	
	/* 	inserting exceptions in the result table for Synonym */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION ID=',a.id, ': Synonym=',a.term, ' contains invalid character.') 	
	from  curr_description_d a , curr_concept_s b 
	where a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and a.typeid ='900000000000013009'
	and term REGEXP '[\\\t\r\n\Z@$]';
	