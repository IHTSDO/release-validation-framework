
/******************************************************************************** 
	file-centric-snapshot-description-unique-FSN

	Assertion:
	Active Fully Specified Name associated with active concepts is unique in DESCRIPTION snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESC: Active FSN =',a.term, ': is not unique in DESCRIPTION snapshot.') 	
	from curr_description_s a , curr_concept_s b	
	where a.conceptid = b.id
	and b.active = 1
	and a.active = 1
	and a.typeid = '900000000000003001'
	group by BINARY  a.term
	having count(a.term) > 1 ;