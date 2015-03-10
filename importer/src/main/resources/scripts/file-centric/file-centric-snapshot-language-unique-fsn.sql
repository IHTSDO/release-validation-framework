
/******************************************************************************** 
	file-centric-snapshot-language-unique-fsn

	Assertion:
	Every concept's FSN exists exactly once in each language refset.


	rtu (20130513) todo...
	- revered reverted to the old 'test' terminology - progress this.
	- should be 1 script per assertion
	

********************************************************************************/
	
	
	/* TEST: Concept has FSN that is defined 2+ times for a given refset 
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',c.id, ': Concept has FSN that is defined more than one time within a given refset.') 
		
	from curr_description_s a 
	inner join curr_langrefset_s b on a.id = b.referencedcomponentid 
	inner join curr_concept_s c on a.conceptid = c.id
	where c.active = '1'
	and b.active = '1'
	and a.active = '1'
	and a.typeid = '900000000000003001'
	GROUP BY b.referencedcomponentid, b.refsetid
	having count(b.referencedcomponentid) > 1
	and count(refsetid) > 1;
	*/
	
/* for active concepts edited for the prospective release, active descriptions appear not more than once as active members of each language refset */ 
	create table if not exists tmp_descsedited as
	select a.id 
	from curr_description_d a
	join curr_concept_s b
	on a.conceptid = b.id
	and b.active = 1;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		b.id
	from curr_langrefset_s a
	join tmp_descsedited b
	on a.referencedcomponentid = b.id
	and a.active = 1
	group by refsetid, referencedcomponentid
	having count(refsetid) > 1 
	and count(referencedcomponentid) > 1;

	drop table if exists  tmp_descsedited;
	
	
	
	
	
	/* TEST: Concept does not have an FSN defined */

	/* Get all active FSNs */
	drop table if exists v_curr_fsn;
	create table if not exists v_curr_fsn (INDEX(conceptid)) as
	select conceptid
		from curr_description_s  
		where active = '1'
		and typeid = '900000000000003001';


		
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept does not have an FSN defined.') 
	
	from curr_concept_s a 
	left join v_curr_fsn  b on b.conceptid = a.id
	where a.active = '1'
	and b.conceptid is null;
		
	
	drop table if exists v_curr_fsn;
	
	
	
	
	
	/* TEST: Concept does not have an FSN in any refset */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',c.id, ': Concept does not have an FSN in any refset.') 
	
	from curr_description_s a 
	left join curr_langrefset_s b on a.id = b.referencedcomponentid 
	inner join curr_concept_s c on a.conceptid = c.id
	where b.id is null
	and a.active = '1'	
	and c.active = '1'
	and a.typeid = '900000000000003001';
	
	
	
	
	
	
	
	
	/* TEST: Concept does not have an FSN in each possible refset */
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',c.id, ': Concept does not have an FSN in each possible refset.') 
	
	from curr_langrefset_s a
	inner join curr_description_s b on b.id = a.referencedcomponentid 
	inner join curr_concept_s c on b.conceptid = c.id
	where a.active = '1'
	and b.active = '1'
	and c.active = '1'
	and b.typeid = '900000000000003001'
	GROUP BY c.id
	having count(distinct(a.refsetid)) < (select count(distinct(refsetid)) from curr_langrefset_s);
	
