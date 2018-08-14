
/******************************************************************************** 
	file-centric-snapshot-ch-it-translated-concepts.sql

	Assertion:
	Newly added active concept has one active preferred term in the Swiss Italian language refset.

********************************************************************************/

insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ': has no active preferred term in the Swiss Italian language refset')
	from curr_concept_d a
	where
	 a.active=1
	 and a.moduleid='2011000195101'
	 and not exists ( select b.id from curr_description_s b, curr_langrefset_s c
			where b.id = c.referencedcomponentid
			and b.typeid = '900000000000013009'
			and b.active=1
			and c.active=1
			and c.acceptabilityid = '900000000000548007'
			and c.refsetid = '2031000195108'
			and b.conceptid=a.id);

			
			
insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ': is added in a recent international release but has no active preferred term in the Swiss Italian language refset')
	from curr_concept_s a
	where
	 a.active=1
	and cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime)) from curr_concept_s z where z.moduleid='900000000000207008')
	 and not exists ( select b.id from curr_description_s b, curr_langrefset_s c
			where b.id = c.referencedcomponentid
			and b.typeid = '900000000000013009'
			and b.active=1
			and c.active=1
			and c.acceptabilityid = '900000000000548007'
			and c.refsetid = '2031000195108'
			and b.conceptid=a.id);
