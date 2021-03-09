/********************************************************************************
	file-centric-refsetid-validation-proc.sql

	Assertion:
	Data should have correct refset ID

********************************************************************************/
drop procedure if exists validate_refset_id;
create procedure validate_refset_id(dbname char(255),runid BIGINT, assertionid char(36))
begin
declare no_more_rows integer default 0;
declare tb_name char(255);
declare table_cursor cursor for select table_name from information_schema.tables where table_schema = substring_index(dbname,'.',1) and table_name like '%\_s' and (table_name like '%refset%' or table_name like '%Refset%');
declare continue handler for not found set no_more_rows = 1;
open table_cursor;
myloop: loop fetch table_cursor into tb_name;
if no_more_rows = 1
then close table_cursor;
leave myloop;
end if;
set @component = substring_index(tb_name,'_s',1);
set @details = CONCAT('CONCAT(\'',@component,'\',\' ::id= \',a.id,\' ::refset id: \',a.refsetid,\' is not in refset dependency list\')');
set @sql = CONCAT('insert into qa_result(run_id, assertion_id,concept_id, details) select ', runid,',',assertionid,',0,',@details,' from (select id,refsetid from ', substring_index(dbname,'.',1),'.',tb_name, '  where refsetid not in (select refsetid from ',substring_index(dbname,'.',1),'.refset_id)) a;');
prepare stmt from @sql;
execute stmt;
drop prepare stmt;
end loop myloop;
end;
