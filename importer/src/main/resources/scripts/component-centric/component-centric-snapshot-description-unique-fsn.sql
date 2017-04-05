/******************************************************************************** 
	component-centric-snapshot-description-unique-fsn

	Assertion:
	The FSN term should be unique in active content.
	Note: As there are lots of FSNs in the past release that don't apply to this rule therefore
	this assertion is only to validate contents added for new concepts during each release.
********************************************************************************/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('FSN=',a.term, ' concept=',a.conceptid, ': FSN term is not unique in description snapshot')
		from curr_description_s a,
		(select b.term, count(distinct b.id) as total from curr_description_s b, (select distinct c.term from curr_concept_d d join curr_description_s c
		on c.conceptid=d.id where d.active=1 and c.active=1 and c.typeid ='900000000000003001') fsn,
		curr_concept_s e
		where e.id=b.conceptid and e.active=1 and b.active =1 and b.term =fsn.term group by b.term having total>1 ) duplicateFsn
		where a.active=1
		and a.term =duplicateFsn.term;