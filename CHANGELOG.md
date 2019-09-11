WC_Custom Changelog (auto-generated)

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
