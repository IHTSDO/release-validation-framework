
/******************************************************************************** 
	file-centric-snapshot-be-translated-descriptions-both-french-dutch.sql

	Assertion:
	An active description must not have preferred term in both Dutch and French language refset.

********************************************************************************/


insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Description: id=',a.id, ': has preferred term in both French and Dutch language refset.')
	from curr_description_s a
        left join curr_langrefset_s b on a.id = b.referencedcomponentid
        left join curr_concept_s c on a.conceptid = c.id
	where a.moduleid='11000172109'
	and (a.languagecode='nl' or a.languagecode='fr')
	and a.typeid='900000000000013009'
	and (b.refsetid='21000172104' or b.refsetid='31000172101')
	and a.active=1
	and b.active=1 
	and b.acceptabilityid='900000000000548007'
	and b.referencedcomponentid is not null
	and a.active=1
	group by a.id, a.languagecode, a.conceptid
	having count(a.id) > 1;

	