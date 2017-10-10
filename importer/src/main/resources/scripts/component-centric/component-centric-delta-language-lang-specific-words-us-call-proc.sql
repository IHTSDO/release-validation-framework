
/******************************************************************************** 
	component-centric-delta-language-lang-specific-words-us-call-proc

	Assertion:
	Calling procedure testing that terms that contain EN-US
	language-specific words are in the same US language refset.

********************************************************************************/

	drop table if exists v_curr_delta_us;
	create table v_curr_delta_us (id varchar(36),term varchar(255),conceptid varchar(255)) default charset=utf8;
	insert into v_curr_delta_us
	select distinct a.id, a.term,a.conceptid
		from curr_description_d a 
		inner join curr_langrefset_s b on a.id = b.referencedComponentId
		and a.active = '1'
		and b.active = '1'
		and a.moduleid != '715515008'
		and b.acceptabilityid ='900000000000548007'
		and b.refsetid = '900000000000509007' /* us language refset */
		and a.typeid = '900000000000013009'; /* synonym */
		
	alter table v_curr_delta_us add index idx_vd_id(id);
	alter table v_curr_delta_us add index idx_vd_conceptid(conceptid);
	alter table v_curr_delta_us add FULLTEXT index idx_vd_us (term);

	call  usTerm_procedure(<RUNID>,'<ASSERTIONUUID>');
	