
/******************************************************************************** 
	component-centric-snapshot-description-fsn-uppercase

	Assertion:
	The first letter of the active FSN associated with active concept should be 
	capitalized.

	note: 	due to a large number of exceptions, this implementation is focused on 
			terms edited in the current propspective release

********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESC: id=',a.id, ':First letter of the active FSN of active concept not capitalized.'),
		a.id,
        'curr_description_d'
	from curr_description_d a
		join curr_concept_s b
			on a.conceptid = b.id
			and b.active = a.active
	where a.active = 1
	and a.typeid = '900000000000003001'
	and a.casesignificanceid != '900000000000017005'
	and binary left(a.term,1) != binary upper(left(a.term,1));
	commit;
