## [2.0.2](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v2.0.1...v2.0.2) (2021-10-14)


### Bug Fixes

* 🐛 Fix response types ([#23](https://github.com/CESNET/spreg_oidc_metadata_sync/issues/23)) ([8b006ac](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/8b006ac2b238ba52c14a7ad12a72ee13971ddb0c))

## [2.0.1](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v2.0.0...v2.0.1) (2021-10-14)


### Bug Fixes

* 🐛 wrong order in hybrid full response ([#22](https://github.com/CESNET/spreg_oidc_metadata_sync/issues/22)) ([186206f](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/186206f02845638ed2658ea56d21fc764624439f))

# [2.0.0](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v1.0.0...v2.0.0) (2021-10-05)


### Features

* 🎸 Token expiration based on grant type ([#21](https://github.com/CESNET/spreg_oidc_metadata_sync/issues/21)) ([9f99d5f](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/9f99d5ff736a2642f85f140e8c67f2e481fcc55f))


### BREAKING CHANGES

* 🧨 3 configuration options removed (access_token_timeout, id_token_timeout,
refresh_token_timeout), added new configuration option replacing them
(tokens)

## [v1.0.0]
- Initial release with automated semantic release

## [v0.2.1]
- Fix setting grant types

## [v0.2.0]
- Instead of grant types, sync "flowTypes"
- Removed syncing of response types, these are generated based on flow type
- Corrections in the README and other docs

## [v0.1.0]
- First release

[v1.0.0]: https://github.com/CESNET/spreg_oidc_metadata_sync/tree/v1.0.0
[v1.0.0]: https://github.com/CESNET/spreg_oidc_metadata_sync/tree/v0.2.1
[v0.2.0]: https://github.com/CESNET/spreg_oidc_metadata_sync/tree/v0.2.0
[v0.1.0]: https://github.com/CESNET/spreg_oidc_metadata_sync/tree/v0.1.0
