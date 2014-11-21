drop table if exists cs_concept;
drop table if exists cs_description;
drop table if exists cs_relationship;
drop table if exists cs_gblangrefset;
drop table if exists cs_uslangrefset;
drop table if exists cs_referTorefset;
drop table if exists cs_nonhumanrefset;
drop table if exists cs_vtmrefset;
drop table if exists cs_vmprefset;
drop table if exists cs_icdorefset;
drop table if exists cs_degreesynonymy;

create table cs_concept(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   definitionstatusid VARCHAR(36),
   concept_uuid VARCHAR(36),
   isDefined_uuid VARCHAR(36),
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18)
);


create table cs_description(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   conceptId VARCHAR(36),
   languageCode VARCHAR(36),
   typeId VARCHAR(36),
   term VARCHAR(255),
   caseSignificanceId VARCHAR(36),
   description_uuid VARCHAR(36),
   concept_uuid VARCHAR(36),
   type_uuid VARCHAR(36),
   isCaseSig_uuid VARCHAR(36),
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18)
);


create table cs_relationship(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   sourceId VARCHAR(36),
   destinationId VARCHAR(36),
   relationshipGroup VARCHAR(36),
   typeId VARCHAR(36),
   characteristicTypeId VARCHAR(36),
   modifierId VARCHAR(36),
   relationship_uuid VARCHAR(36),
   source_uuid VARCHAR(36),
   target_uuid VARCHAR(36),
   type_uuid VARCHAR(36),
   characteristic_uuid VARCHAR(36),
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18)
);

create table cs_gblangrefset(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName VARCHAR(255),
   concept VARCHAR(36),
   refCompId VARCHAR(36),
   refCompType VARCHAR(36),
   refset_uuid VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);

create table cs_uslangrefset(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName	VARCHAR(255),
   concept	VARCHAR(36),
   refCompId	VARCHAR(36),
   refCompType	VARCHAR(36),
   refset_uuid	VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);

create table cs_referTorefset(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName	VARCHAR(255),
   concept	VARCHAR(36),
   refCompId	VARCHAR(36),
   refCompType	VARCHAR(36),
   refset_uuid	VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);

create table cs_nonhumanrefset(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName	VARCHAR(255),
   concept	VARCHAR(36),
   refCompId	VARCHAR(36),
   refCompType	VARCHAR(36),
   refset_uuid	VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);


create table cs_vtmrefset(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName	VARCHAR(255),
   concept	VARCHAR(36),
   refCompId	VARCHAR(36),
   refCompType	VARCHAR(36),
   refset_uuid	VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);

create table cs_vmprefset(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName	VARCHAR(255),
   concept	VARCHAR(36),
   refCompId	VARCHAR(36),
   refCompType	VARCHAR(36),
   refset_uuid	VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);


create table cs_icdorefset(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName	VARCHAR(255),
   concept	VARCHAR(36),
   refCompId	VARCHAR(36),
   refCompType	VARCHAR(36),
   refset_uuid	VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);

create table cs_degreesynonymy(
   id VARCHAR(36) not null,
   effectivetime CHAR(8),
   active CHAR(1),
   refsetId VARCHAR(36),
   refsetName	VARCHAR(255),
   concept	VARCHAR(36),
   refCompId	VARCHAR(36),
   refCompType	VARCHAR(36),
   refset_uuid	VARCHAR(36),
   concept_uuid	VARCHAR(36),
   refCompId_uuid VARCHAR(36) ,
   author VARCHAR(36),
   path VARCHAR(255),
   commitTime VARCHAR(18),
   type VARCHAR(36),
   value VARCHAR(36) 
);


