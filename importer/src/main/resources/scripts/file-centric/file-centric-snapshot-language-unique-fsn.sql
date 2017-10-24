
/******************************************************************************** 
	file-centric-snapshot-language-unique-fsn

	Assertion:
	Every concept's FSN exists exactly once in each language refset.


	rtu (20130513) todo...
	- revered reverted to the old 'test' terminology - progress this.
	- should be 1 script per assertion
	

********************************************************************************/
	
	
	/* TEST: Recently edited Concept has FSN that is defined 2+ times for a given refset*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		c.id,
		concat('Concept: id=',c.id, ' has an FSN that is defined more than one time within a given refset.') 
	from curr_description_d a 
	inner join curr_langrefset_s b on a.id = b.referencedcomponentid 
	inner join curr_concept_s c on a.conceptid = c.id
	where c.active = '1'
	and b.active = '1'
	and a.active = '1'
	and a.typeid = '900000000000003001'
	GROUP BY c.id,b.referencedcomponentid, b.refsetid
	having count(b.referencedcomponentid) > (select count(distinct(refsetid)) from curr_langrefset_s);
	
	
/* for active concepts edited for the prospective release, active descriptions appear not more than once as active members of each language refset */ 
	create table if not exists tmp_descsedited as
	select a.id,a.conceptid 
	from curr_description_d a
	join curr_concept_s b
	on a.conceptid = b.id
	and b.active = 1;

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		b.conceptid,
		concat('Description: id=',b.id,' is active and appears more than once within a given refset.')
	from curr_langrefset_s a
	join tmp_descsedited b
	on a.referencedcomponentid = b.id
	and a.active = 1
	group by b.conceptid, refsetid, referencedcomponentid
	having count(referencedcomponentid) > (select count(distinct(refsetid)) from curr_langrefset_s);
	
	drop table if exists tmp_descsedited;
	
	
	/* TEST: Concept does not have an FSN defined */

	/* Get all active FSNs */
	drop table if exists v_curr_fsn;
	create table if not exists v_curr_fsn (INDEX(conceptid)) as
	select conceptid
		from curr_description_s  
		where active = '1'
		and typeid = '900000000000003001';
		
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' does not have an FSN defined.') 
	from curr_concept_s a 
	left join v_curr_fsn  b on b.conceptid = a.id
	where a.active = '1'
	and b.conceptid is null;
	
	drop table if exists v_curr_fsn;
	
	/* TEST: Concept does not have an FSN in any refset */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' does not have an active FSN in any refset.') 
	from curr_concept_s a
	where a.active = '1'
	and not exists (select b.id
		from curr_description_s b, curr_langrefset_s c
		where b.id=c.referencedcomponentid
		and b.active = '1'
		and c.active = '1'
		and b.typeid = '900000000000003001'
		and b.conceptid= a.id);

	
	/* TEST: Concept does not have an FSN in the US language refset */
	/*Only for core module concepts*/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('Concept: id=',a.id, ' does not have an FSN preferred in the en-US language refset.') 
	from curr_concept_s a
	where a.active = '1'
	and a.moduleid in ('900000000000207008','900000000000012004')
	and not exists (select b.id
		from curr_description_s b, curr_langrefset_s c
		where b.id=c.referencedcomponentid
		and b.active = '1'
		and c.active = '1'
		and b.typeid = '900000000000003001'
		and c.refsetid='900000000000509007' 
		and c.acceptabilityid='900000000000548007'
		and b.conceptid= a.id);

	

	
