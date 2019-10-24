WC_Custom Changelog (auto-generated)

# [3.36.0](https://github.com/smtadmin/WC_Custom/compare/3.35.0...3.36.0) (2019-10-24)


### Bug Fixes

* **feature-wsla-i95:** fixed a bug where dates didnt progress in a logical pattern ([0e8da38](https://github.com/smtadmin/WC_Custom/commit/0e8da38))
* **ST-62:** Add fd snapshot table. ([2da0a00](https://github.com/smtadmin/WC_Custom/commit/2da0a00))


### Features

* **feature-wsla-i87:** add customer product id to select search ([07ca0e1](https://github.com/smtadmin/WC_Custom/commit/07ca0e1))
* clean up ([151688e](https://github.com/smtadmin/WC_Custom/commit/151688e))

# [3.35.0](https://github.com/smtadmin/WC_Custom/compare/3.34.0...3.35.0) (2019-10-17)


### Bug Fixes

* **WSLA-Migration:** Add archive package with varied POJOs to archive the raw XLS data extracted fro ([bc6b120](https://github.com/smtadmin/WC_Custom/commit/bc6b120))
* **WSLA-Migration:** Profeco importer ([eac71ec](https://github.com/smtadmin/WC_Custom/commit/eac71ec))
* **WSLA-Migration:** Progress on Profeco tickets.  Correct ticket Originators ([c72187b](https://github.com/smtadmin/WC_Custom/commit/c72187b))


### Features

* **Events:** Added filtering to the events page ([57095cc](https://github.com/smtadmin/WC_Custom/commit/57095cc))
* **Performance:** Converted events to be a servcer side pagination to improve performance ([da74de5](https://github.com/smtadmin/WC_Custom/commit/da74de5))

# [3.34.0](https://github.com/smtadmin/WC_Custom/compare/3.33.1...3.34.0) (2019-10-11)


### Bug Fixes

* **Profeco:** Status change on the profeco is not being recognized. ([0121eab](https://github.com/smtadmin/WC_Custom/commit/0121eab))
* **Status:** Updated the profeco status to return a default value of NO_PROFECO instead of n ull or ([1c475d3](https://github.com/smtadmin/WC_Custom/commit/1c475d3))


### Features

* **Communicant:** Added child communicant values for the incoming/outgoing email and phone communic ([c43dd59](https://github.com/smtadmin/WC_Custom/commit/c43dd59))
* **feature-wsla-i87:** open logistics movement to customer ser. role ([95ad079](https://github.com/smtadmin/WC_Custom/commit/95ad079))
* **Profeco:** Added the profeco work panel ([0064f6f](https://github.com/smtadmin/WC_Custom/commit/0064f6f))
* **WSLA-I86:** Added the ability to store the comm role for an activity ([779caab](https://github.com/smtadmin/WC_Custom/commit/779caab))

## [3.33.1](https://github.com/smtadmin/WC_Custom/compare/3.33.0...3.33.1) (2019-10-10)


### Bug Fixes

* **Sc-1560:** Ensure that suffixes are not lost when setting thousands seperators. ([5fe207c](https://github.com/smtadmin/WC_Custom/commit/5fe207c))
* **Sc-230:** Change to use super methods. ([98f585a](https://github.com/smtadmin/WC_Custom/commit/98f585a))
* **Sc-230:** Fix null pointer exception ([3a8e17c](https://github.com/smtadmin/WC_Custom/commit/3a8e17c))
* **Sc-230:** Move defautl tree session caching out of actions. ([79b2985](https://github.com/smtadmin/WC_Custom/commit/79b2985))
* **Sc-230:** Shift caching to system and tree from session to request. ([e23c201](https://github.com/smtadmin/WC_Custom/commit/e23c201))
* **Sc-670:** Ensure that non-text number fields contain thousands seperators. ([7b429fe](https://github.com/smtadmin/WC_Custom/commit/7b429fe))
* **SSO:** Add capture of nameIdPolicy when saving SSO config ([7697deb](https://github.com/smtadmin/WC_Custom/commit/7697deb))

# [3.33.0](https://github.com/smtadmin/WC_Custom/compare/3.32.1...3.33.0) (2019-10-04)


### Bug Fixes

* **feature-wsla-i10:** user caller originator bug fix ([e5f6e47](https://github.com/smtadmin/WC_Custom/commit/e5f6e47))
* **Ticket:** Added the get caller method to fix the ui ([3509520](https://github.com/smtadmin/WC_Custom/commit/3509520))


### Features

* **feature-wsla-i10:** update shipping work panel to speed up page load and limit data going betwee ([9295cee](https://github.com/smtadmin/WC_Custom/commit/9295cee))

## [3.32.1](https://github.com/smtadmin/WC_Custom/compare/3.32.0...3.32.1) (2019-10-02)


### Bug Fixes

* **DS-536:** Ensure expiresAt is pushed when blank, to remove upstream/existing values ([b0fbe00](https://github.com/smtadmin/WC_Custom/commit/b0fbe00))

# [3.32.0](https://github.com/smtadmin/WC_Custom/compare/3.31.0...3.32.0) (2019-10-02)


### Bug Fixes

* **WSLA-Migration:** Profeco import setup.  Activity Comments. ([6560f09](https://github.com/smtadmin/WC_Custom/commit/6560f09))


### Features

* **Misc:** Misc new items for the WSLA launch ([f4fe257](https://github.com/smtadmin/WC_Custom/commit/f4fe257))

# [3.31.0](https://github.com/smtadmin/WC_Custom/compare/3.30.0...3.31.0) (2019-09-29)


### Bug Fixes

* **Debit Memo:** Finalized the importing of the debit memos for end users ([6b812c5](https://github.com/smtadmin/WC_Custom/commit/6b812c5))
* **feature-wsla-i9:** fix sorting in credit and debit memo ([85b9643](https://github.com/smtadmin/WC_Custom/commit/85b9643))
* **Importer:** Adding the debit memos for end users ([4c21eb6](https://github.com/smtadmin/WC_Custom/commit/4c21eb6))
* **Misc Changes:** Misc changes for launch.  Fixed bug when approving CAS that was duplicating the c ([795c4fa](https://github.com/smtadmin/WC_Custom/commit/795c4fa))


### Features

* **Debit Memos:** Finished importer for user debit memos as well as mofigied the debit memo query ([c9be720](https://github.com/smtadmin/WC_Custom/commit/c9be720))
* **Debit Memos:** Totalled the credit memos and added the amount to the debit memo.  This cleans up ([7441d29](https://github.com/smtadmin/WC_Custom/commit/7441d29))
* **feature-wsla-i9:** update debit memo job, action to support the diffent types of debit memo proc ([fc21b0b](https://github.com/smtadmin/WC_Custom/commit/fc21b0b))
* **Importer:** Working throuogh the importer ([b7d7e30](https://github.com/smtadmin/WC_Custom/commit/b7d7e30))
* **Iomporter:** changes to the importer ([7fa9e39](https://github.com/smtadmin/WC_Custom/commit/7fa9e39))


### Performance Improvements

* **feature-wsla-i9:** update ([c9de893](https://github.com/smtadmin/WC_Custom/commit/c9de893))

# [3.30.0](https://github.com/smtadmin/WC_Custom/compare/3.29.2...3.30.0) (2019-09-27)


### Bug Fixes

* **feature-wsla-i9:** fix bug where billable amounts were not appearing on time line ([e278aee](https://github.com/smtadmin/WC_Custom/commit/e278aee))
* **feature-wsla-i9:** fix flow so end users do not get changed for uploading their own photos but ca ([419b046](https://github.com/smtadmin/WC_Custom/commit/419b046))
* **feature-wsla-i9:** fix for packing slip pdf ([e630449](https://github.com/smtadmin/WC_Custom/commit/e630449))
* **feature-wsla-i9:** fix keep disposed location of a disposed tv ([9f7327e](https://github.com/smtadmin/WC_Custom/commit/9f7327e))
* **feature-wsla-i9:** Fix on final save for a new ticket check to see what billable activities have ([cbca817](https://github.com/smtadmin/WC_Custom/commit/cbca817))
* **feature-wsla-i9:** if unit is destroyed leave it destroyed. ([7c45d48](https://github.com/smtadmin/WC_Custom/commit/7c45d48))
* **feature-wsla-i9:** when a set is decommisioned make sure the sn can no longer be used ([1a876ee](https://github.com/smtadmin/WC_Custom/commit/1a876ee))
* **MTS-i22 Site Search:** Exclude unpublished articles from site-search results ([8598b95](https://github.com/smtadmin/WC_Custom/commit/8598b95))
* **WSLA-Migration:** Complete Harvest workflow ([8c86cdc](https://github.com/smtadmin/WC_Custom/commit/8c86cdc))
* **WSLA-Migration:** Finish Replacement workflow.  Run importer through all legacy data ([65ed6bc](https://github.com/smtadmin/WC_Custom/commit/65ed6bc))


### Features

* **Display:** When displaying the product in a selectpicker, concatenate the product name and sku ([3f96be1](https://github.com/smtadmin/WC_Custom/commit/3f96be1))
* **feature-wsla-i9:** Add diagnostics to cloned ticket, fix data bug ([61ef925](https://github.com/smtadmin/WC_Custom/commit/61ef925))
* **feature-wsla-i9:** add feature to mark if a customer(retailer) owns the refund process ([184ef71](https://github.com/smtadmin/WC_Custom/commit/184ef71))
* **feature-wsla-i9:** change from cost to invoice ([8068f96](https://github.com/smtadmin/WC_Custom/commit/8068f96))
* **Importer:** Added a Debit Memo Importer ([a8dc3b1](https://github.com/smtadmin/WC_Custom/commit/a8dc3b1))
* **Return Refused:** Added capability for when a user refuses to have the unit returned. ([4a30a82](https://github.com/smtadmin/WC_Custom/commit/4a30a82))
* **Survey:** Added the results of a survey to the lookup action ([5d0f70a](https://github.com/smtadmin/WC_Custom/commit/5d0f70a))
* **Survey Wrapper:** Added a custom post processr for the form creation.  Tied ticket number to the ([f35169a](https://github.com/smtadmin/WC_Custom/commit/f35169a))
* **Survey Wrapper:** Created a custom post-processor for the survey/forms to connect the form submi ([1974457](https://github.com/smtadmin/WC_Custom/commit/1974457))
* **WSLA I50:** Added the standing code changes when a user refuses the return of the unit ([2b095c8](https://github.com/smtadmin/WC_Custom/commit/2b095c8))
* **WSLA i54 and i55:** Added management for the product validation when added by call center. Added ([5b39ed7](https://github.com/smtadmin/WC_Custom/commit/5b39ed7))

## [3.29.2](https://github.com/smtadmin/WC_Custom/compare/3.29.1...3.29.2) (2019-09-24)


### Bug Fixes

* **MTS-i22:** Enforce Issue and Article publish dates to public users ([67da862](https://github.com/smtadmin/WC_Custom/commit/67da862))
* **Sc-1505:** Add ability to load a limited version of an external page via local iframe. ([750f7a6](https://github.com/smtadmin/WC_Custom/commit/750f7a6))
* **Sc-1505:** Add comments and split out retrieve into multiple methods. ([6a63d93](https://github.com/smtadmin/WC_Custom/commit/6a63d93))
* **Sc-1505:** Fix issues with requiring cookies enabled. ([e5d52bf](https://github.com/smtadmin/WC_Custom/commit/e5d52bf))
* **WSLA Migration:** Complete Asset Parser.  Phase 1 - open tickets importing (code complete) ([ec59550](https://github.com/smtadmin/WC_Custom/commit/ec59550))

## [3.29.1](https://github.com/smtadmin/WC_Custom/compare/3.29.0...3.29.1) (2019-09-17)


### Bug Fixes

* **SC-1614:** Fixed unchecked NPE that could occur in Feeds. ([c7832d2](https://github.com/smtadmin/WC_Custom/commit/c7832d2))

# [3.29.0](https://github.com/smtadmin/WC_Custom/compare/3.28.0...3.29.0) (2019-09-13)


### Bug Fixes

* **Email Comments:** discard e-mails without a job id in the reference ([287e5cc](https://github.com/smtadmin/WC_Custom/commit/287e5cc))
* **feature-wsla-i6:** remove unneeded cde ([8d0c05c](https://github.com/smtadmin/WC_Custom/commit/8d0c05c))
* **Sc-1487:** Ensure preview selections are passed along with redirects. ([91656f6](https://github.com/smtadmin/WC_Custom/commit/91656f6))
* **Sc-1596:** Add product id to the product explorer report. ([bec8499](https://github.com/smtadmin/WC_Custom/commit/bec8499))
* **Sc-1604:** Add ability to set max documents to index at once via the maxDocsIndex property in con ([10e289a](https://github.com/smtadmin/WC_Custom/commit/10e289a))
* **WSLA-I25:** add additional rules for processing the Standing Code job ([0a0ce24](https://github.com/smtadmin/WC_Custom/commit/0a0ce24))
* **WSLA-I25:** create nightly job to update ticket standings ([78b9dd3](https://github.com/smtadmin/WC_Custom/commit/78b9dd3))
* **WSLA-I25:** update service orders to critical in a batch query ([ed4c8f9](https://github.com/smtadmin/WC_Custom/commit/ed4c8f9))


### Features

* **Billable and cost:** Modified the billable activities to support a cost and invoice default.  Mo ([a62b1ed](https://github.com/smtadmin/WC_Custom/commit/a62b1ed))
* **Defaults:** Added default values for cost and invoice amount when a warranty is created ([46d6b35](https://github.com/smtadmin/WC_Custom/commit/46d6b35))
* **feature-wsla-i6:** add new status for call center review ([f10e706](https://github.com/smtadmin/WC_Custom/commit/f10e706))
* **feature-wsla-i6:** minor bug fix and drag drop now filters ([2488bb1](https://github.com/smtadmin/WC_Custom/commit/2488bb1))
* **Filtering:** Added the ability to filter the shipments by weather or not they are assigned to a ([8512547](https://github.com/smtadmin/WC_Custom/commit/8512547))

# [3.28.0](https://github.com/smtadmin/WC_Custom/compare/3.27.0...3.28.0) (2019-09-11)


### Bug Fixes

* **DSEMEA-62:** Correct equality test to use primitive values rather than object footprint ([19cc2d5](https://github.com/smtadmin/WC_Custom/commit/19cc2d5))
* **DSEMEA-62:** Finish expiresAt implementation to Showpad API ([72383dd](https://github.com/smtadmin/WC_Custom/commit/72383dd))


### Features

* **DSEMEA-62:** Add Expiration Date to Showpad/Mediabin processing ([7889c31](https://github.com/smtadmin/WC_Custom/commit/7889c31))

# [3.27.0](https://github.com/smtadmin/WC_Custom/compare/3.26.0...3.27.0) (2019-09-09)


### Bug Fixes

* **MTS-i16:** Default ordering to lastLogin, descending ([5d6099a](https://github.com/smtadmin/WC_Custom/commit/5d6099a))
* **MTS-i23:** Fix issue with nullable fKey.  Fix optional user fields not saving/displaying ([88b52ab](https://github.com/smtadmin/WC_Custom/commit/88b52ab))


### Features

* **IP Security:** Modified the IP Security stuff to allow ip ranges to include class B and class C ([a6e2881](https://github.com/smtadmin/WC_Custom/commit/a6e2881))

# [3.26.0](https://github.com/smtadmin/WC_Custom/compare/3.25.0...3.26.0) (2019-09-05)


### Bug Fixes

* Correct typo on update_dt annotation ([51a82d8](https://github.com/smtadmin/WC_Custom/commit/51a82d8))
* **MTS-i13 SSO:** Fix delete support.  Ensure site config is deleted too ([f6d0c17](https://github.com/smtadmin/WC_Custom/commit/f6d0c17))
* **MTS-i13 SSO:** Fix NPE ([b65793d](https://github.com/smtadmin/WC_Custom/commit/b65793d))
* **Sc-1596:** Fix issue where growth did not match group total. ([4cb0aaa](https://github.com/smtadmin/WC_Custom/commit/4cb0aaa))


### Features

* **MTS-i13 SSO:** Add SSO support to the site, with /portal mgmt UI ([95cf7e6](https://github.com/smtadmin/WC_Custom/commit/95cf7e6))

# [3.25.0](https://github.com/smtadmin/WC_Custom/compare/3.24.5...3.25.0) (2019-08-30)


### Bug Fixes

* **Sc-1581:** Add account permissions summary report. ([4cef038](https://github.com/smtadmin/WC_Custom/commit/4cef038))
* **Security:** disallowed access to the site for logins with an active flag of 0 ([48e63cc](https://github.com/smtadmin/WC_Custom/commit/48e63cc))
* **WSLA-Migration:** Include product lookup across the alias fields ([ba695b9](https://github.com/smtadmin/WC_Custom/commit/ba695b9))


### Features

* **Command Line:** Modified the Content Feed Job to run form the command line.  This allows for the ([d0d8fb9](https://github.com/smtadmin/WC_Custom/commit/d0d8fb9))
* **Filter:** Addd a fliter to the feed so only medtech strategist publication areticles are being s ([f3e1387](https://github.com/smtadmin/WC_Custom/commit/f3e1387))

## [3.24.5](https://github.com/smtadmin/WC_Custom/compare/3.24.4...3.24.5) (2019-08-29)


### Bug Fixes

* **DS-527:** Trap exception caused by isValid test and reopen them safely ([46ec621](https://github.com/smtadmin/WC_Custom/commit/46ec621))
* **Sc-1566:** Allow sections to have differing current quarters and make manage and public current q ([40fb44f](https://github.com/smtadmin/WC_Custom/commit/40fb44f))
* **Sc-1578:** Change enum name to match parent attribute id properly. ([1e97b6b](https://github.com/smtadmin/WC_Custom/commit/1e97b6b))
* **Sc-1586:** Add analyses to the link tool's search. ([b5e65d5](https://github.com/smtadmin/WC_Custom/commit/b5e65d5))
* **WSLA-Migration:** Combine Southware comments into a single Cypher comment ([a668adb](https://github.com/smtadmin/WC_Custom/commit/a668adb))
* **WSLA-Migration:** Import Ticket Comments ([72ff661](https://github.com/smtadmin/WC_Custom/commit/72ff661))
* **WSLA-Migration:** Save comment1 as tkt description.  Save the rest as tkt comments ([e61303c](https://github.com/smtadmin/WC_Custom/commit/e61303c))

## [3.24.4](https://github.com/smtadmin/WC_Custom/compare/3.24.3...3.24.4) (2019-08-23)


### Bug Fixes

* **Connection:** Modified the SFTP connection timeout to be reduced to 3 seconds and to exit more gr ([9e719af](https://github.com/smtadmin/WC_Custom/commit/9e719af))
* **feature-wsla-i6:** fix to allow view of opened ticketed ([f1435da](https://github.com/smtadmin/WC_Custom/commit/f1435da))
* **Job:** Added code to exit the job if no articles are found ([70f75c5](https://github.com/smtadmin/WC_Custom/commit/70f75c5))
* **Security:** Using the passed in IP address instead of the req remote address ([ae4651e](https://github.com/smtadmin/WC_Custom/commit/ae4651e))

## [3.24.3](https://github.com/smtadmin/WC_Custom/compare/3.24.2...3.24.3) (2019-08-21)


### Bug Fixes

* **Sc-1584:** Chang eto use EnumUtil. ([5ece437](https://github.com/smtadmin/WC_Custom/commit/5ece437))
* **Sc-1584:** Fix issue with solr indexing. ([950912b](https://github.com/smtadmin/WC_Custom/commit/950912b))
* **Sc-1584:** Return empty checks ([1d6fca1](https://github.com/smtadmin/WC_Custom/commit/1d6fca1))

## [3.24.2](https://github.com/smtadmin/WC_Custom/compare/3.24.1...3.24.2) (2019-08-16)


### Bug Fixes

* **DePuy IFU:** Monitor dbConns for timeout and re-open as needed ([3636756](https://github.com/smtadmin/WC_Custom/commit/3636756))
* **DePuy-IFU:** Transpose IFU_GROUP_ID.  Create wc_sync records ([9c18a93](https://github.com/smtadmin/WC_Custom/commit/9c18a93))

## [3.24.1](https://github.com/smtadmin/WC_Custom/compare/3.24.0...3.24.1) (2019-08-14)


### Bug Fixes

* **Cancel:** Modified the flow when a new article is cancelled to remove the article form the mts do ([bd2ac96](https://github.com/smtadmin/WC_Custom/commit/bd2ac96))
* **Security:** Made sure that an issue and the documents are approved before displaying on the site ([2626206](https://github.com/smtadmin/WC_Custom/commit/2626206))
* **Security and Authors:** Fixed a login bug where an author wasn't getting loaded into the userdata ([8cf35ee](https://github.com/smtadmin/WC_Custom/commit/8cf35ee))

# [3.24.0](https://github.com/smtadmin/WC_Custom/compare/3.23.0...3.24.0) (2019-08-12)


### Bug Fixes

* **Auth:** Changed the query to get the assigned publications.  Added a field to the query for the m ([827497e](https://github.com/smtadmin/WC_Custom/commit/827497e))
* **Sc-1506:** Add year_no and market group id column. ([463f058](https://github.com/smtadmin/WC_Custom/commit/463f058))
* **Sc-1537:** Set grid row details when loading all grids. ([c55c267](https://github.com/smtadmin/WC_Custom/commit/c55c267))


### Features

* **SC-1278:** Initial Solr Integrations for Feeds UI Updates. ([daa4ff5](https://github.com/smtadmin/WC_Custom/commit/daa4ff5))

# [3.23.0](https://github.com/smtadmin/WC_Custom/compare/3.22.3...3.23.0) (2019-08-09)


### Features

* **Misc:** Misc cleanup before launch ([2061094](https://github.com/smtadmin/WC_Custom/commit/2061094))

## [3.22.3](https://github.com/smtadmin/WC_Custom/compare/3.22.2...3.22.3) (2019-08-07)


### Bug Fixes

* **Search:** Fixed search to include author now that the author is not in the title ([00bafee](https://github.com/smtadmin/WC_Custom/commit/00bafee))

## [3.22.2](https://github.com/smtadmin/WC_Custom/compare/3.22.1...3.22.2) (2019-08-06)


### Bug Fixes

* **Display:** Fixed the display of categories to onbly show the heading if the category has items as ([a0e9819](https://github.com/smtadmin/WC_Custom/commit/a0e9819))

## [3.22.1](https://github.com/smtadmin/WC_Custom/compare/3.22.0...3.22.1) (2019-08-02)


### Bug Fixes

* **Preview:** Forgot to add the page preview token to the preview URL ([1b5524d](https://github.com/smtadmin/WC_Custom/commit/1b5524d))

# [3.22.0](https://github.com/smtadmin/WC_Custom/compare/3.21.2...3.22.0) (2019-07-29)


### Bug Fixes

* **FileUpload:** Tell FileLoader to re-orient uploaded user files ([2e5a4ff](https://github.com/smtadmin/WC_Custom/commit/2e5a4ff))
* **Related:** Related articles didn't dislay articles int he BS Table if the author was unassigned. ([96f9d58](https://github.com/smtadmin/WC_Custom/commit/96f9d58))
* **WSLA Migration:** Progress on Migration Scenarios - unfinished at this stage ([47099f2](https://github.com/smtadmin/WC_Custom/commit/47099f2))
* **WSLA Migration:** Unit defects, repair types and operators ([fd150b0](https://github.com/smtadmin/WC_Custom/commit/fd150b0))


### Features

* **Misc:** Misc cleanup based upon feedback from MTS ([5a06cc2](https://github.com/smtadmin/WC_Custom/commit/5a06cc2))
* **Users:** Added the ability to filter by publication on the users page ([10e7f03](https://github.com/smtadmin/WC_Custom/commit/10e7f03))
* **Users:** Added the capability to check for the existance of a user before adding them ([0e7e0fa](https://github.com/smtadmin/WC_Custom/commit/0e7e0fa))
