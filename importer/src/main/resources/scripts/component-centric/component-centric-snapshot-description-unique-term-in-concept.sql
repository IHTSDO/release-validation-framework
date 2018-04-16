/******************************************************************************** 
	component-centric-snapshot-description-unique-term-in-concept

	Assertion:
	For a given concept, all active description terms are unique.

	Implementation is limited to active descriptions of active concepts edited 
	in the current prospective release.
	Note: See another assertion component-centric-snapshot-description-unique-terms
********************************************************************************/
/*  violators have the same term twice within a concept */

drop table if exists tmp_description_edited;
create table if not exists tmp_description_edited (
	id bigint(20) not null,
	languagecode varchar(2) not null,
	conceptid bigint(20) not null,
	term varchar(256) not null,
	semantictag varchar(100));
	
insert into tmp_description_edited (id, languagecode, conceptid, term) 
select a.id, a.languagecode, a.conceptid, a.term
	from curr_description_s a
	where a.active =1
	and exists (select id from res_edited_active_concepts where id = a.conceptid);
	
alter table tmp_description_edited add index idx_tmp_ds_cid (conceptId);
alter table tmp_description_edited add index idx_tmp_ds_l (languagecode);
alter table tmp_description_edited add index idx_tmp_ds_t (term);
	
insert into qa_result (runid, assertionuuid, concept_id, details)
select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.conceptid,
	concat('Description: Id=', a.id, ' contains non-unique term ', a.term) 
from tmp_description_edited a
group by a.conceptid, a.languagecode, a.term
having count(distinct a.id) > 1;