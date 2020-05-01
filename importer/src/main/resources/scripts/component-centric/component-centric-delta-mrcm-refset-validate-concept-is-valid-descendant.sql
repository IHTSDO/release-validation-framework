-- Common checks
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_d','moduleId ','mrcmAttributeDomainRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeRangeRefset_d','moduleId ','mrcmAttributeRangeRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmDomainRefset_d','moduleId ','mrcmDomainRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmModuleScopeRefset_d','moduleId ','mrcmModuleScopeRefset',
'900000000000443000', '< 900000000000443000 |Module|');
-- MRCM Domain Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmDomainRefset_d','refsetId ','mrcmDomainRefset',
'723589008', '< 723589008 |MRCM domain reference set|');
-- MRCM Attribute Domain Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_d','refsetId ','mrcmAttributeDomainRefset',
'723604009', '< 723604009 |MRCM attribute domain reference set|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_d','referencedComponentId','mrcmAttributeDomainRefset',
'410662002', '< 410662002 |Concept model attribute|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_d','ruleStrengthId','mrcmAttributeDomainRefset',
'723573005', '< 723573005 |Concept model rule strength|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeDomainRefset_d','contentTypeId','mrcmAttributeDomainRefset',
'723574004', '< 723574004 |Content type|');
-- MRCM Attribute Range Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeRangeRefset_d','refsetId','mrcmAttributeRangeRefset',
'723592007', '< 723592007 |MRCM attribute range reference set|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmAttributeRangeRefset_d','referencedComponentId','mrcmAttributeRangeRefset',
'410662002', '< 410662002 |Concept model attribute|');
-- MRCM Attribute Module Scope Refset checks
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmModuleScopeRefset_d','referencedComponentId','mrcmModuleScopeRefset',
'900000000000443000', '< 900000000000443000 |Module|');
call validateConceptIdIsValidDescendantsInMRCMRefsetDelta_procedure(<RUNID>,'<ASSERTIONUUID>',
'mrcmModuleScopeRefset_d','mrcmRuleRefsetId','mrcmModuleScopeRefset',
'723589008,723604009,723592007', '( < 723589008 |MRCM domain reference set| OR < 723604009 |MRCM attribute domain reference set| OR < 723592007 |MRCM attribute range reference set|)');

