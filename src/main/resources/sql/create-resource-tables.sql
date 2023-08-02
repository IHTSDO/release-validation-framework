drop table if exists res_us_gb_terms;
create table res_us_gb_terms (
	us_term VARCHAR(1000),
	gb_term VARCHAR(1000)
)engine=myisam default charset=utf8;
create index idx_res_usTermMap on res_us_gb_terms(us_term);
create index idx_res_gbTermMap on res_us_gb_terms(gb_term);


drop table if exists res_casesensitiveTerm;

create table res_casesensitiveTerm(
   casesensitiveTerm VARCHAR(1000) not null
)engine=myisam default charset=utf8;
create index idx_casesensitiveTerm on res_casesensitiveTerm(casesensitiveTerm);

