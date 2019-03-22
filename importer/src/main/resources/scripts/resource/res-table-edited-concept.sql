
/*	
	creates a resource table of concept SCITDs of all concepts for which any component has been 
	editied in the current release 
*/

	SET tmp_table_size = 1024 * 1024 * 1024 * 2;
	SET max_heap_table_size = 1024 * 1024 * 1024 * 2;

	drop table if exists res_concepts_edited;
	create table if not exists res_concepts_edited(conceptid varchar(36), key idx_conceptid (conceptid)) ENGINE=MEMORY;
	truncate table res_concepts_edited;
    
	insert into res_concepts_edited
	select distinct id
	from curr_concept_s
	where id in ( 
		select id from curr_concept_d
    union 
        select conceptid from curr_description_d
    union 
        select conceptid from curr_textdefinition_d
    union 
        select sourceid from curr_stated_relationship_d
    union
        select referencedcomponentid from curr_owlexpressionrefset_d
    union
        select b.conceptid from curr_langrefset_d a 
        left join curr_description_s b on a.referencedcomponentid=b.id
    union 
        select referencedcomponentid from curr_attributevaluerefset_d
        where refsetid = '900000000000489007'
    union 
        select a.conceptid from curr_description_d a
        join curr_attributevaluerefset_d b on a.id = b.referencedcomponentid
        where b.refsetid = '900000000000490003'
	union
        select referencedcomponentid
        from curr_associationrefset_d
        where refsetid in ('900000000000527005','900000000000523009','900000000000526001','900000000000528000')
	union
        select a.conceptid from curr_description_d a 
        join curr_associationrefset_d b on a.id = b.referencedcomponentid 
        where b.refsetid = '900000000000531004'
	union
        select referencedcomponentid from curr_simplerefset_d
	union
        select referencedcomponentid from curr_simplemaprefset_d
    );
    commit;
    
    drop table if exists res_edited_active_concepts;
	create table if not exists res_edited_active_concepts
	(index idx_reac_id (id))
	as select b.*
	from res_concepts_edited a 
		join curr_concept_s b
			on a.conceptid = b.id
			and b.active = 1;
	commit;
    
     