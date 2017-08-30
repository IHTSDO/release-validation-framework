-- Common checks
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_f','moduleId ','mrcmAttributeDomainRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeRangeRefset_f','moduleId ','mrcmAttributeRangeRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmDomainRefset_f','moduleId ','mrcmDomainRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmModuleScopeRefset_f','moduleId ','mrcmModuleScopeRefset',
'900000000000443000', '< 900000000000443000 |Module|');
-- MRCM Domain Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmDomainRefset_f','refsetId ','mrcmDomainRefset',
'723589008', '< 723589008 |MRCM domain reference set|');
-- MRCM Attribute Domain Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_f','refsetId ','mrcmAttributeDomainRefset',
'723604009', '< 723604009 |MRCM attribute domain reference set|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_f','referencedComponentId','mrcmAttributeDomainRefset',
'410662002', '< 410662002 |Concept model attribute|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_f','ruleStrengthId','mrcmAttributeDomainRefset',
'723573005', '< 723573005 |Concept model rule strength|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_f','contentTypeId','mrcmAttributeDomainRefset',
'723574004', '< 723574004 |Content type|');
-- MRCM Attribute Range Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeRangeRefset_f','refsetId','mrcmAttributeRangeRefset',
'723592007', '< 723592007 |MRCM attribute range reference set|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeRangeRefset_f','referencedComponentId','mrcmAttributeRangeRefset',
'410662002', '< 410662002 |Concept model attribute|');
-- MRCM Attribute Module Scope Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmModuleScopeRefset_f','referencedComponentId','mrcmModuleScopeRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetFull_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmModuleScopeRefset_f','mrcmRuleRefsetId','mrcmModuleScopeRefset',
'723589008,723604009,723592007', '( < 723589008 |MRCM domain reference set| OR < 723604009 |MRCM attribute domain reference set| OR < 723592007 |MRCM attribute range reference set|)');

