WC_Custom Changelog (auto-generated)

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
