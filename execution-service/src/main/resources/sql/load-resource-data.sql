load data local 
	infile '<data_location>/cs_words.txt'
	into table res_casesensitiveTerm
	columns terminated by '\t' 
	lines terminated by '\n' 
	ignore 1 lines;
	
load data local 
	infile '<data_location>/usTerms.txt'
	into table res_usterm
	columns terminated by '\t' 
	lines terminated by '\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/gbTerms.txt'
	into table res_gbterm
	columns terminated by '\t' 
	lines terminated by '\n' 
	ignore 1 lines;

load data local 
	infile '<data_location>/semanticTags.txt'
	into table res_semantictag
	columns terminated by '\t' 
	lines terminated by '\n'
	ignore 1 lines;