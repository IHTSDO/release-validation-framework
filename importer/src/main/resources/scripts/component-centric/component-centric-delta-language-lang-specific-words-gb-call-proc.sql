
/******************************************************************************** 
	component-centric-delta-language-lang-specific-words-gb-call-proc

	Assertion:
	Calling procedure testing that terms that contain EN-GB 
	language-specific words are in the same GB language refset.
	Note:

********************************************************************************/

	drop table if exists v_curr_delta_gb;
	create table if not exists v_curr_delta_gb
	(	id varchar(36),
		term varchar(255),
		conceptid varchar(255)
	) ENGINE=MyISAM;
	
	Insert into v_curr_delta_gb
	select distinct a.id, a.term,a.conceptid
		from curr_description_d a 
		inner join curr_langrefset_s b on a.id = b.referencedComponentId
		and a.active = '1'
		and b.active = '1'
		and a.moduleid != '715515008'
		and b.acceptabilityid ='900000000000548007'	
		and b.refsetid = '900000000000508004' /* GB English */
		and a.typeid = '900000000000013009';	/* synonym */
		
	alter table v_curr_delta_gb add index idx_vd_id(id);
	alter table v_curr_delta_gb add index idx_vd_conceptid(conceptid);
	alter table v_curr_delta_gb add FULLTEXT index idx_vd_gb (term);

	call gbTerm_procedure(<RUNID>,'<ASSERTIONUUID>');
	