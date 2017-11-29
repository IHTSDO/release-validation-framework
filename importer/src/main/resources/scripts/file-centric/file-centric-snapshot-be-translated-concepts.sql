
/******************************************************************************** 
	file-centric-snapshot-be-translated-concepts.sql

	Assertion:
	Newly translated concepts must have preferred terms in Dutch and French.

********************************************************************************/


insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Concept: id=',a.conceptid, ': has preferred term in French but not in Dutch.')
	from curr_description_d a left join curr_langrefset_s b on a.id= b.referencedcomponentid 
	where a.languagecode='fr' 
	and a.moduleid='11000172109'
	and a.typeid='900000000000013009'
	and b.refsetid='21000172104'
	and a.active =1
	and b.active=1 
	and b.acceptabilityid='900000000000548007'
	and b.referencedcomponentid is not null
	and not exists (
			select c.id from curr_description_s c left join curr_langrefset_s d on c.id= d.referencedcomponentid 
			where c.languagecode='nl' 
			and c.moduleid='11000172109'
			and c.typeid='900000000000013009'
			and c.conceptid= a.conceptid 
			and d.refsetid='31000172101'
			and d.acceptabilityid='900000000000548007'
			and c.active =1 
			and d.active=1 
			and d.referencedcomponentid is not null);

	
insert into qa_result (runid, assertionuuid, concept_id, details)
	select  	
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Concept: id=',a.conceptid, ': has preferred term in Dutch but not in French.')
	from curr_description_d a left join curr_langrefset_s b on a.id= b.referencedcomponentid 
	where a.languagecode='nl' 
	and a.moduleid='11000172109'
	and a.typeid='900000000000013009'
	and b.refsetid='31000172101'
	and a.active =1
	and b.active=1 
	and b.acceptabilityid='900000000000548007'
	and b.referencedcomponentid is not null
	and not exists (
			select c.id from curr_description_s c left join curr_langrefset_s d on c.id= d.referencedcomponentid 
			where c.languagecode='fr' 
			and c.moduleid='11000172109'
			and c.typeid='900000000000013009'
			and c.conceptid= a.conceptid 
			and d.refsetid='21000172104'
			and d.acceptabilityid='900000000000548007'
			and c.active =1 
			and d.active=1 
			and d.referencedcomponentid is not null);
	