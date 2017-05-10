/******************************************************************************** 
	component-centric-snapshot-description-unique-fsn

	Assertion:
	The FSN term should be unique in active content.
	Note: This assertion is to report any duplicate FSNs.(case sensitive)
********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.conceptid,
		concat('FSN=',c.term, ' descriptionid=',c.id, ': FSN term is not unique in description snapshot')
		from 
		(select b.term as fsn, count(distinct b.conceptid) as counter from curr_concept_s a, curr_description_s b where a.id=b.conceptid and a.active=1 and b.active=1 and b.typeid='900000000000003001' 
		group by b.term having counter > 1) duplicateFsn,
		curr_description_s c
		where c.active=1
		and c.term=duplicateFsn.fsn;
