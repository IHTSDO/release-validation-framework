load data local 
	infile '<data_location>/us-to-gb-terms-map.txt'
	into table res_us_gb_terms
	columns terminated by '\t' 
	lines terminated by '\n' 
	ignore 1 lines;
