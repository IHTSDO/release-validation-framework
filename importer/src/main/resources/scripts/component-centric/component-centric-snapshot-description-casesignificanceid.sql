
/******************************************************************************** 
	component-centric-snapshot-description-casesignificanceid

	Assertion:
	Case-sensitive terms have appropriate caseSignificanceId.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESC: id=',a.id, ':Case-sensitivity terms containing inappropriate caseSignificanceId.')
	from  curr_description_d a , curr_concept_s b,res_casesensitiveTerm c 
	where a.casesignificanceid != 900000000000017005
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and a.term like c.casesensitiveTerm group by a.id;
	