Download and Store Published Releases
===================
RVF requires at least one published release before running any validations. 
Generally this will be the relevant version of the SNOMED International Edition that your own extension/content depends on.

Note: You don't need to download the international release if your package is already an edition package.

You have the following options to store published release files:

1. Create a subfolder /store/releases under the release-validation-framework directory to store published releases.
```
mkdir store/releases
```

2. Or add a soft link to an existing directory where you have published releases downloaded already.
```
ln -s /path/to/your/release_files store/releases
```
