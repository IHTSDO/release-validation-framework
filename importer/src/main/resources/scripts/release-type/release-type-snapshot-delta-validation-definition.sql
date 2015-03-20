/******************************************************************************** 
	release-type-SNAPSHOT-delta-validation-Definition

	Assertion:
	The current data in the Definition snapshot file are the same as the data in 
	the current delta file. 
********************************************************************************/

  drop table if exists td;
  create table if not exists td like curr_textdefinition_s;
  insert into td
	select *
	from curr_textdefinition_s
	where cast(effectivetime as datetime) >
	(select max(cast(effectivetime as datetime))
	 from prev_textdefinition_f);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('DEFINITION: id=',a.id, ': Concept in snapshot file, but not in delta file.')
	from  td a
	left join curr_textdefinition_d b
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
		concat('DEFINITION: id=',a.id, ': Concept in delta but not in snapshot file.') 	
	from curr_textdefinition_d a
	left join td b
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


	drop table if exists td;
	 
	 
	 
	 