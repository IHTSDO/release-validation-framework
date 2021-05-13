
/******************************************************************************** 
	file-centric-snapshot-description-space-before-tag

	Assertion:
	All active FSNs associated with active concepts have a space before the semantic tag.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('DESCRIPTION: id=',a.id, ' : The FSN has no space before the semantic tag.'),
		a.id,
        'curr_description_s'
	from curr_description_s a , curr_concept_s b
	where typeid ='900000000000003001'
	and a.active = 1
	and b.active = 1
	and a.conceptid = b.id
	and a.term not like '% (%';
	commit;
	