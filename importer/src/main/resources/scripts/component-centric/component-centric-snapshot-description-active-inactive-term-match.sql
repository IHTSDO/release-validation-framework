
/******************************************************************************** 
	component-centric-snapshot-description-active-inactive-term-match

	Assertion:
	No active term associated with active concept matches that of an inactive 
	description.

	Note: 	many violations of this assertion were created in prior SNOMED CT 
			releases. Consequently this implementation focuses on highlighting 
			new violations created in the currently prospective release.

	no term of an active description of a concept of which a description was edited 
	matches that of an inactive description within the same concept

	Note: case sensitive.

********************************************************************************/

/* 	limit to a list of active concepts of which descriptions have been edited 
	this release 
*/
	drop table if exists tmp_edited_con;
	create table if not exists tmp_edited_con as
	select distinct a.*
	from curr_concept_s a
		join curr_description_d b
			on a.id = b.conceptid
			and a.active = 1;
	commit;

/* list of active description of active concepts edited for this release */
	drop table if exists tmp_active_desc;
	create table if not exists tmp_active_desc as
	select a.*
	from curr_description_d a 
	where a.active=1 and not exists (select count(*) as total from curr_description_f b where a.id=b.id having total > 1);
	commit;
	
	alter table tmp_active_desc add index idx_tmp_ad_cid(conceptid);
	alter table tmp_active_desc add index idx_tmp_ad_a(active);
	alter table tmp_active_desc add index idx_tmp_ad_t(term);

/* list of inactive description of active concepts edited for this release */
	drop table if exists tmp_inactive_desc;
	create table if not exists tmp_inactive_desc as
	select a.*
	from curr_description_s a
		join tmp_edited_con b
			on a.conceptid = b.id
			and a.active = 0;
	commit;
	
	alter table tmp_inactive_desc add index idx_tmp_id_cid(conceptid);
	alter table tmp_inactive_desc add index idx_tmp_id_a(active);
	alter table tmp_inactive_desc add index idx_tmp_id_t(term);

/* 	violators are active descriptions of which the terms are the same as 
	inactive descriptions for a given concept 
*/ 
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.conceptid,
		concat('Active description id = ',a.id,' and inactive description id = ', b.id, ' share the same term') 
	from tmp_active_desc a
	join tmp_inactive_desc b
	on a.conceptid = b.conceptid
	and a.moduleid = b.moduleid
	and cast(a.term as binary)= cast(b.term as binary)
	where a.active != b.active
	and cast(a.effectivetime as datetime) >= cast(b.effectivetime as datetime);
	commit;

	drop table if exists tmp_edited_con;
	drop table if exists tmp_active_desc;
	drop table if exists tmp_inactive_desc;
