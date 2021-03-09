
/********************************************************************************
	file-centric-snapshot-correct-refset-id.sql

	Assertion:
	The refset id of all data must be correct

********************************************************************************/
drop table if exists refset_id;
create table refset_id(
refsetid bigint(20) not null
) engine=myisam default charset=utf8;

insert into refset_id
select distinct referencedcomponentid from curr_refsetdescriptor_s
where active = 1;


call validate_refset_id('<PROSPECTIVE>',<RUNID>,'<ASSERTIONUUID>');
drop table if exists refset_id;