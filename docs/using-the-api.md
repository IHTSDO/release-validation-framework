
Using the rest api
--------------------

The RVF API is documented using Swagger and is found here - http://localhost:8081/api/swagger-ui.html

The steps to validate an extension or edition are as follows:

1. [Download Published Releases](download-published-releases.md)
2. [Understanding Validation Assertions](#understanding-validation-assertions)
3. [Validate Your Content](#validate-your-content)
4. [Examples](#examples)

Understanding Validation Assertions
-----------------------------------

The RVF uses _assertions_ to validate content and is pre-loaded with the assertions used by SNOMED International to validate the International Edition of SNOMED CT as well as some of the other extensions and editions.

This list can be seen at <http://localhost:8081/api/assertions>, where you will see that there are a lot. This is where **assertion groups** come in handy, <http://localhost:8081/api/groups>, listing assertion groups based upon the relevant validation context.

Assertion groups that are most common are:

|Assertion Group Name | Description |
|:------------- |:------------- |
| **common-authoring** | A common group of assertions when validating a snapshot and not just a delta (this is likely to be the most common one to use) |
| **common-edition** |  A common group of assertions that will do the generic assertion tests that will work across most extensions/editions |
| **first-time-common-edition** | The common-edition group of assertions as above but specifically for a first time extension, i.e one that has not yet been published or is the first published version |

The names of other groups are fairly self-explanatory and may be applicable depending on what content is being validated.

Validate Your Content
---------------------

Now onto the real stuff... validating your content. This is done with the [run-post end point](http://localhost:8081/api/swagger-ui.html#!/test45upload45file45controller/runPostTestPackageUsingPOST).

The following parameters should be used:

|Parameter | Value | Description | 
|:------------- |:------------- |:------------- |
| file | `<filename>` | The RF2 zip file containing the content to be validated. This must contain the relevant release type being validated ([delta](https://confluence.ihtsdotools.org/display/DOCGLOSS/delta+release) or [snapshot](https://confluence.ihtsdotools.org/display/DOCGLOSS/snapshot+release)). |
| rf2DeltaOnly | false | This is set to true when a [delta release](https://confluence.ihtsdotools.org/display/DOCGLOSS/delta+release) is being validated. This should be false if validating a [snapshot release](https://confluence.ihtsdotools.org/display/DOCGLOSS/snapshot+release). |
| writeSuccesses | false | This indicates on whether the final report should list the successes. Due to the large amount of data in the international RF2 files, this should be generally set to false. |
| groups | `<assertion_groups>` | The assertion groups to run as part of the validation, shown [above](#select-assertions) . You can list a different group if required, and separate multiple groups using commas. For most cases, first-time-common-edition **or** common-edition **or** common-authoring would be the ones to choose from. |
| previousRelease | `<previous_release>` | Leave it empty when validating without previous release (i.e using assertion group first-time-common-edition) Otherwise enter the previous release version identified by the filename of the release you have previously downloaded (using the example above, this could be `SNOMEDCT_RF2_20200301T110000Z.zip`) |
| dependencyRelease | `<dependency_release>` | The dependent International release version file name (e.g `SnomedCT_InternationalRF2_PRODUCTION_20190731T120000Z.zip`) used for validating extensions only. Note: Leave this empty when validating International release files.|
| runId | `<numeric identifier>` | Enter either the timestamp for point that this validation is being run or any numeric identifier to be used to identify the job (e.g. `2020090101`). |
| storageLocation | `<sub_foldername>` | The folder where validation reports will be saved to. This needs to have a value and the rvf application must have permissions to write to this location. e.g. `int/20190131/test`|

Before starting the validation job, please ensure that the release packages (.zip) specified in the parameters (i.e. _previousRelease_, _dependencyRelease_, and _previousDependencyEffectiveTime_) are downloaded [as described here](download-published-releases.md).

Once the parameters have been entered, submit the POST request and wait. The validation is likely to take some time but the job status can be found via the results URL. The URL of these RVF validation results can be found in the response location header parameter:

e.g. `:"location": "http://localhost:8081/api/result/201905010901?storageLocation=int/20190131/test"`

When complete,the results report is returned, formatted in JSON at the same URL.

Examples
--------
In this example, we apply [SQL assertions](https://github.com/IHTSDO/snomed-release-validation-assertions/) only (i.e. without  [MRCM](https://confluence.ihtsdotools.org/display/DOCGLOSS/MRCM) and [Drools rules](https://github.com/IHTSDO/snomed-drools-rules))
1. Validating an International Edition release package 
```
curl -X 'POST' \
  'http://localhost:8081/api/run-post?rf2DeltaOnly=false&writeSuccesses=false&groups=InternationalEdition&previousRelease=SnomedCT_InternationalRF2_PRODUCTION_20240901T120000Z.zip&runId=1727827200&failureExportMax=10&storageLocation=int%2F1727827200&enableDrools=false&releaseAsAnEdition=false&standAloneProduct=false&enableMRCMValidation=false&enableTraceabilityValidation=false&enableChangeNotAtTaskLevelValidation=false' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@SnomedCT_InternationalRF2_PRODUCTION_20241001T120000Z.zip;type=application/zip' \
  -F 'manifest='
```

In the following examples, we enable [MRCM](https://confluence.ihtsdotools.org/display/DOCGLOSS/MRCM) and [Drools rules](https://github.com/IHTSDO/snomed-drools-rules) using additional parameters (please refer to [Swagger](http://localhost:8081/api/swagger-ui.html) for details).

2. Validating an International Edition release package
```
curl -X 'POST' \
  'http://localhost:8081/api/run-post?rf2DeltaOnly=false&writeSuccesses=false&groups=InternationalEdition&droolsRulesGroups=common-authoring%2Cint-authoring&previousRelease=SnomedCT_InternationalRF2_PRODUCTION_20240901T120000Z.zip&runId=1727827200&failureExportMax=10&storageLocation=int%2F1727827200&enableDrools=true&effectiveTime=2024-10-01&releaseAsAnEdition=false&standAloneProduct=false&enableMRCMValidation=true&enableTraceabilityValidation=false&enableChangeNotAtTaskLevelValidation=false' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@SnomedCT_InternationalRF2_PRODUCTION_20241001T120000Z.zip;type=application/zip' \
  -F 'manifest='
```

3. Validating a National Extension release package (e.g. Austrian Extension)
```
curl -X 'POST' \
  'http://localhost:8081/api/run-post?rf2DeltaOnly=false&writeSuccesses=false&groups=common-edition&groups=at-authoring&droolsRulesGroups=common-authoring&droolsRulesGroups=at-authoring&previousRelease=SnomedCT_ManagedServiceAT_PRODUCTION_AT1000234_20240215T120000Z.zip&dependencyRelease=SnomedCT_InternationalRF2_PRODUCTION_20240701T120000Z.zip&runId=1723766400&failureExportMax=10&storageLocation=at%2F1723766400&enableDrools=true&effectiveTime=2024-08-15&releaseAsAnEdition=false&standAloneProduct=false&defaultModuleId=11000234105&includedModules=11000234105&enableMRCMValidation=true&enableTraceabilityValidation=false&enableChangeNotAtTaskLevelValidation=false' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@SnomedCT_ManagedServiceAT_PRODUCTION_AT1000234_20240815T120000Z.zip;type=application/zip' \
  -F 'manifest='
```

4. Validating a National Edition release package (e.g. Netherlands Edition). Note that _previousDependencyEffectiveTime_ is required and _releaseAsAnEdition_ should be set to `true` for Edition release packages.
```
curl -X 'POST' \
  'http://localhost:8081/api/run-post?rf2DeltaOnly=false&writeSuccesses=false&groups=common-edition&groups=nl-authoring&droolsRulesGroups=common-authoring&droolsRulesGroups=nl-authoring&previousRelease=SnomedCT_ManagedServiceNL_PRODUCTION_NL1000146_20240831T120000Z.zip&dependencyRelease=SnomedCT_InternationalRF2_PRODUCTION_20240901T120000Z.zip&previousDependencyEffectiveTime=2024-08-01&runId=1727740800&failureExportMax=10&storageLocation=nl%2F1727740800&enableDrools=true&effectiveTime=2024-09-30&releaseAsAnEdition=true&standAloneProduct=false&defaultModuleId=11000146104&includedModules=11000146104&enableMRCMValidation=true&enableTraceabilityValidation=false&enableChangeNotAtTaskLevelValidation=false' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@SnomedCT_ManagedServiceNL_PRODUCTION_NL1000146_20240930T120000Z.zip;type=application/zip' \
  -F 'manifest='
```