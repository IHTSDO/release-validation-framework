drop table if exists res_us_gb_terms;
create table res_us_gb_terms (
	us_term	varchar(255),
	gb_term	varchar(255)
)engine=myisam default charset=utf8;
create index idx_res_usTermMap on res_us_gb_terms(us_term);
create index idx_res_gbTermMap on res_us_gb_terms(gb_term);


drop table if exists res_casesensitiveTerm;

create table res_casesensitiveTerm(
   casesensitiveTerm VARCHAR(255) not null
)engine=myisam default charset=utf8;
create index idx_casesensitiveTerm on res_casesensitiveTerm(casesensitiveTerm);

