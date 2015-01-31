
/******************************************************************************** 
	component-centric-snapshot-description-valid characters

	Assertion:
	Active Terms of active concept consist of valid characters.

********************************************************************************/
	
/* 	view of current snapshot made by finding all the active term for active concepts containing invalid character */
	
	drop table if exists v_curr_snapshot;
	create table if not exists  v_curr_snapshot as
	select a.id
	from  curr_description_d a , curr_concept_s b 
	where a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and ( 
	term like '.'
	or term like '\t'
	or term like '\r'
	or term like '\n'
	or term like '\a'
	or term like '\$'
	or term like '\0'
	or term like '\''
	or term like '\''
	or term like '\"'
	or term like '\b'
	or term like '\Z'
	or term like '\\'
	or term like '\%'		
	);
	
	
/* 	inserting exceptions in the result table */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: id=',a.id, ':Invalid character in active terms of active concept.') 	
	from v_curr_snapshot a;


	drop table if exists v_curr_snapshot;
	

	