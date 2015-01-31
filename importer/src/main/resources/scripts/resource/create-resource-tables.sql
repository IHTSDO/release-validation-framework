/* 
	For each resource table:
		1) Drop table if exists
		2) Create table
		3) Create Indices (if necessary)
*/
drop table if exists res_navigationconcept;
create table res_navigationconcept (
	 id		char(36),
	 effectiveTime	char(8),
	 active		char(1),
	 moduleId	bigint,
	 refsetId	bigint,
	 referencedComponentId	bigint,
	 ordernum	int,
 	linkedTo	bigint
)engine=myisam default charset=utf8;
create index idx_navconcepts on res_navigationconcept(referencedComponentId);


drop table if exists res_gbterm;

create table res_gbterm (
	term	varchar(255)
);
create index idx_gbtermsTerm on res_gbterm(term);


drop table if exists res_usterm;

create table res_usterm (
	term	varchar(255)
);
create index idx_ustermsTerm on res_usterm(term);


drop table if exists res_semantictag;

create table res_semantictag(
   semantictag VARCHAR(255) not null,      
   id VARCHAR(36) not null      
);


drop table if exists res_casesensitiveTerm;

create table res_casesensitiveTerm(
   casesensitiveTerm VARCHAR(255) not null
);
create index idx_casesensitiveTerm on res_casesensitiveTerm(casesensitiveTerm);
