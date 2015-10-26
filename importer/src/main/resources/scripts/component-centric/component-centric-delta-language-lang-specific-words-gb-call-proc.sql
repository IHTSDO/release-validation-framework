
/******************************************************************************** 
	component-centric-delta-language-lang-specific-words-gb-call-proc

	Assertion:
	Calling procedure testing that terms that contain EN-GB 
	language-specific words are in the same GB language refset.
	Note:

********************************************************************************/

	drop table if exists v_curr_delta_gb;
	create table if not exists v_curr_delta_gb as
	select distinct a.id, a.term,a.conceptid
		from curr_description_d a 
		inner join curr_langrefset_s b on a.id = b.referencedComponentId
		and a.active = '1'
		and b.active = '1'
		and b.acceptabilityid ='900000000000548007'	
		and b.refsetid = '900000000000508004' /* GB English */
		and a.typeid = '900000000000013009';	/* synonym */	

	call gbTerm_procedure(<RUNID>,'<ASSERTIONUUID>');
	