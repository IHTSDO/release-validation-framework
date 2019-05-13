Working with the RVF
====================

Getting to the API
--------------------

The RVF API is documented using Swagger and is found here - http://localhost:8081/api/swagger-ui.html

The steps to validate an extension or edition are as follows:

1. [Upload a published release](#upload-a-published-release)
2. [Select the assertions you want to run by selecting an assertion group](#select-assertions)
3. [Upload and test your extension/edition](#validate-your-content)


Upload a Published Release
--------------------------

The RVF needs at least one published release loaded, and at least the release that your own extension/content depends upon.

The release endpoint of the REST API can be used to list releases and to upload a published release.
Find the endpoint at **http://localhost:8081/api/releases**

Example upload (**noting the release name and release date at the end of the url**):

```bash
curl -X POST -F 'file=@SnomedCT_RF2Release_INT_20190131.zip' http://localhost:8081/api/releases/int/20190131
```

Once the upload has completed, go and check at http://localhost:8081/api/releases and you should see the recently uploaded release listed

Select Assertions
-----------------

The RVF uses assertions to validate content and is pre-loaded with the assertions used by SNOMED International to validate the International Edition of SNOMED CT as well as other extensions and editions.

This list can be seen at http://localhost:8081/api/assertions where you'll find quite a few. This is where assertion groups come in handy, http://localhost:8081/api/groups, listing assertions based upon the validation use case.

Assertions that are more useful in the first instance are:

|Assertion Group Name | Description | 
|:------------- |:------------- |
| common-edition |  A common group of assertions that will do the generic assertion tests that will work across most extensions/editions |
| first-time-common-edition | The common group of assertions as above but specifically for a first time extension, i.e one that has not yet been published or is the first published version |

The names of other groups are fairly self-explanatory and may be applicable depending on what content is being validated.

Validate Your Content
---------------------

Now onto the real stuff... validating content. This is done with the [run-post end point](http://localhost:8081/api/swagger-ui.html#!/test45upload45file45controller/runPostTestPackageUsingPOST).

The following parameters should be used:

|Parameter | Value | Description | 
|:------------- |:------------- |:------------- |
| file | `<filename>` | The RF2 zip file containing the content to be validated |
| rf2DeltaOnly | false | This is set to true when a _delta_ set of content is being validated. Generally this should be false |
| writeSuccesses | false | This indicates on whether the final report should list the successes. Due to the large amount of data in the international RF2 files, this should be generally set to false. |
| groups | first-time-common-edition **or** common-edition | The assertion groups to run as part of the validation. Obviously, you can list a different group if required |
| previousRelease | `<previous_release>` | Leave it empty when validating without previous release (i.e using assertion group first-time-common-edition) Otherwise enter the previous release version |
| dependencyRelease | `<dependency_release>` | The dependent international release version (e.g rvf_int_20190131) used for validating extensions only. This is the name of the release first uploaded and listed at http://localhost:8081/api/releases Note: Leave this empty when validating the international release files.|
| runId | `<201905010901>` | Enter the timestamp for point that this validation is being run to be used as the job id |
| storageLocation | `<sub_foldername>` | The folder where validation reports will be saved to. This needs to have a value. eg. int/20190131/test|

Once those have been entered, then submit the post and wait. The validation is likely to take some time but the job status can be found via the results URL.

The RVF validation results polling URL can be found in the response location header parameter.

e.g :"location": "http://localhost:8081/api/result/201905010901?storageLocation=int/20190131/test"

