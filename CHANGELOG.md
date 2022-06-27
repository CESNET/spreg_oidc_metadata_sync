## [3.1.9](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.8...v3.1.9) (2022-06-27)


### Bug Fixes

* **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v2.7.1 ([e73df5f](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/e73df5f6f9fd196783843951e9a7e114d7b56c40))

## [3.1.8](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.7...v3.1.8) (2022-06-01)


### Bug Fixes

* **deps:** update dependency com.nimbusds:nimbus-jose-jwt to v9.23 ([417063c](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/417063ced01c1ee1c1535677b35f0b94f7c9cbb3))

## [3.1.7](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.6...v3.1.7) (2022-05-23)


### Bug Fixes

* **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v2.7.0 ([b7ea368](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/b7ea3688ec7d18a68f741fae8d5d735905cfa862))

## [3.1.6](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.5...v3.1.6) (2022-04-25)


### Bug Fixes

* **deps:** update dependency com.nimbusds:nimbus-jose-jwt to v9.22 ([6586bb2](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/6586bb27231b2009c1aaf113d708609b45cef18f))

## [3.1.5](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.4...v3.1.5) (2022-04-25)


### Bug Fixes

* **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v2.6.7 ([0bfc68c](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/0bfc68c86d492528e66e9735fb7576b0fca80c17))
* **deps:** update dependency org.springframework.security.oauth:spring-security-oauth2 to v2.5.2.release ([548d796](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/548d796250734e9b3209b9a6f0fd2e6eb64290ac))

## [3.1.4](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.3...v3.1.4) (2022-03-31)


### Bug Fixes

* **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v2.6.6 ([c3a5e68](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/c3a5e684e7bc34e70ad8adf7a9c9dc7fbd5f4bd1))

## [3.1.3](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.2...v3.1.3) (2022-03-09)


### Bug Fixes

* **deps:** update dependency com.nimbusds:nimbus-jose-jwt to v9.21 ([17a12a5](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/17a12a5155e8347d6ee092d335c2eaa7088ada8a))
* **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v2.6.4 ([00ef7bb](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/00ef7bb241f1c06b80afbac6b98d5455b3d09830))

## [3.1.2](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.1...v3.1.2) (2022-02-21)


### Bug Fixes

* **deps:** update dependency com.nimbusds:nimbus-jose-jwt to v9.19 ([d50997c](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/d50997cda5fe4a9419286f60968efb6067b81659))

## [3.1.1](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.1.0...v3.1.1) (2022-02-21)


### Bug Fixes

* **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v2.6.3 ([458277b](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/458277b9caab82177032668fab6436f683f81f29))

# [3.1.0](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.0.1...v3.1.0) (2022-01-26)


### Features

* 🎸 Sync reuse of refresh tokens setting ([a10ddd4](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/a10ddd4740384ca1d32d47ddb71baef087fed862))

## [3.0.1](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v3.0.0...v3.0.1) (2022-01-24)


### Bug Fixes

* 🐛 End prematurely if Peurn has thrown an exception ([2607a5e](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/2607a5e1af167da1e6648a59161e2ba9d386a190))

# [3.0.0](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v2.1.1...v3.0.0) (2022-01-19)


### Features

* 🎸 By default rotate refresh tokens ([8dcab3b](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/8dcab3badf8ff84912299e64c4ea6d144a098cf1))


### BREAKING CHANGES

* 🧨 For some RPs, this might be breaking, if they rely on reusing the
refresh token.

## [2.1.1](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v2.1.0...v2.1.1) (2021-11-22)


### Bug Fixes

* 🐛 Limit pool size to 1 connection only ([42680ed](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/42680ed4be78093007d635b5a30e61083627b992))

# [2.1.0](https://github.com/CESNET/spreg_oidc_metadata_sync/compare/v2.0.2...v2.1.0) (2021-11-22)


### Features

* 🎸 Enable PostgreSQL ([92d6cc6](https://github.com/CESNET/spreg_oidc_metadata_sync/commit/92d6cc6818de6caae2dfb2696c46074f9a32f5db))

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
