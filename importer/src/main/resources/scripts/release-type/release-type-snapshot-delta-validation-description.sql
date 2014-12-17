/******************************************************************************** 
	release-type-SNAPSHOT-delta-validation-Description

	Assertion:
	The current data in the Definition snapshot file are the same as the data in 
	the current delta file. 
********************************************************************************/

  drop table if exists des;
  create table if not exists des like curr_description_s;
  insert into des
	select *
	from curr_description_s
	where cast(effectivetime as datetime)= 
	(select max(cast(effectivetime as datetime))
	 from curr_description_s);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESCRIPTION: id=',a.id, ': Concept in snapshot file, but not in delta file.')
	from  des a
	left join curr_description_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.conceptid = b.conceptid
		and a.languagecode = b.languagecode
		and a.typeid = b.typeid
		and a.term = b.term
		and a.casesignificanceid = b.casesignificanceid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.conceptid is null
	or b.languagecode is null
	or b.typeid is null
	or b.term is null
	or b.casesignificanceid is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DESCRIPTION: id=',a.id, ': Concept in delta but not in snapshot file.') 	
	from curr_description_d a
	left join des b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.conceptid = b.conceptid
		and a.languagecode = b.languagecode
		and a.typeid = b.typeid
		and a.term = b.term
		and a.casesignificanceid = b.casesignificanceid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.conceptid is null
	or b.languagecode is null
	or b.typeid is null
	or b.term is null
	or b.casesignificanceid is null;


	drop table if exists des;
	 
	 
	 
	 