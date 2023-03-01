# Importing SNOMED Release Validation Assertions

RVF assertions are in github [snomed-release-validation-assertions](https://github.com/IHTSDO/snomed-release-validation-assertions)

## Clone snomed-release-validation-assertions in the parent folder of release-validation-framework

```bash
git clone https://github.com/IHTSDO/snomed-release-validation-assertions.git
```
## Import assertions on startup by default

RVF imports assertions on startup from following location:
```properties
rvf.assertion.resource.local.path=../snomed-release-validation-assertions/
```

## Reload the latest assertions

RVF skips importing assertions if they exist already. To reload the latest assertions, the easiest way is to log onto mysql and recreate rvf_master db.

```sql
drop database rvf_master;
create database rvf_master;
```
