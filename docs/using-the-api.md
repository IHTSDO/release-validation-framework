
Using the rest api
--------------------

The RVF API is documented using Swagger and is found here - http://localhost:8081/api/swagger-ui.html

The steps to validate an extension or edition are as follows:

1. [Download Published Releases](download-published-releases.md)
2. [Understanding Validation Assertions](#understanding-validation-assertions)
3. [Validate Your Content](#validate-your-content)

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
| previousRelease | `<previous_release>` | Leave it empty when validating without previous release (i.e using assertion group first-time-common-edition) Otherwise enter the previous release version identified by the filename of the release you have previously uploaded (using the example above, this could be `SNOMEDCT_RF2_20200301T110000Z.zip`) |
| dependencyRelease | `<dependency_release>` | The dependent International release version file name (e.g `SnomedCT_InternationalRF2_PRODUCTION_20190731T120000Z.zip`) used for validating extensions only. This is the name of the file used for the release first uploaded and listed at <http://localhost:8081/api/releases>. Note: Leave this empty when validating the international release files.|
| runId | `<numeric identifier>` | Enter either the timestamp for point that this validation is being run or any numeric identifier to be used to identify the job (e.g. `2020090101`). |
| storageLocation | `<sub_foldername>` | The folder where validation reports will be saved to. This needs to have a value and the rvf application must have permissions to write to this location. eg. `int/20190131/test` or `/tmp` |

Please ensure that the releases specified in the parameters (e.g. _previousRelease_, _dependencyRelease_, and _previousDependencyEffectiveTime_) are made available to RVF [as described here](download-published-releases.md).

Once those have been entered, then submit the post and wait. The validation is likely to take some time but the job status can be found via the results URL. The URL of these RVF validation results can be found in the response location header parameter:

e.g `:"location": "http://localhost:8081/api/result/201905010901?storageLocation=int/20190131/test"`

When complete,the results report is returned, formated in JSON at the same URL.
