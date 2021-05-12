
/******************************************************************************** 
	file-centric-snapshot-description-fsn-tag

	Assertion:
	All active FSNs associated with active concept have a semantic tag.

********************************************************************************/	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESC: Term=',a.term, ':Fully Specified Name without semantic tag.'),
		a.id,
        'curr_description_s'
	from curr_description_s a , curr_concept_s b
	where a.typeid ='900000000000003001'
	and a.active = 1
	and a.term not like '% (%)'
	and a.conceptid = b.id
	and b.active = 1;
	commit;
	