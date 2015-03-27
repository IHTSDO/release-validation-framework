
/******************************************************************************** 
	component-centric-delta-language-lang-specific-words-us-call-proc

	Assertion:
	Calling procedure testing that terms that contain EN-US
	language-specific words are in the same US language refset.

********************************************************************************/

	drop table if exists v_curr_delta;
	create table if not exists v_curr_delta as
	select distinct a.id, a.term
		from curr_description_d a 
		inner join curr_langrefset_s b on a.id = b.referencedComponentId
		and a.active = '1'
		and b.active = '1'
		and b.acceptabilityid ='900000000000548007'
		and b.refsetid = '900000000000509007' /* us language refset */
		and a.typeid = '900000000000013009'; /* synonym */		

	call  usTerm_procedure(<RUNID>,'<ASSERTIONUUID>');
	drop table if exists v_curr_delta;
	