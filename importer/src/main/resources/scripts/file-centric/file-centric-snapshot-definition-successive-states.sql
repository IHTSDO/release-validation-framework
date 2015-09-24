
/******************************************************************************** 
	file-centric-snapshot-description-successive-states

	Assertion:	
	New inactive states follow active states in the DEFINITION snapshot.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('TEXTDEF: id=',a.id, ':New inactive states follow active states in the DEFINITION snapshot.') 	
	from curr_textdefinition_s a , prev_textdefinition_s b
	where cast(a.effectivetime as datetime) >
				(select max(cast(effectivetime as datetime)) 
				 from prev_textdefinition_s)
	and a.active = 0
	and a.id = b.id
	and a.active = b.active;