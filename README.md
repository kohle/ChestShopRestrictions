# ChestShopRestrictions
![ChestShopRestrictions latest release version](https://img.shields.io/github/v/release/kohle/ChestShopRestrictions)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/kohle/ChestShopRestrictions/ci.yml)
![ChestShopRestrictions latest release downloads](https://img.shields.io/github/downloads/kohle/ChestShopRestrictions/latest/total)
![ChestShopRestrictions total downloads](https://img.shields.io/github/downloads/kohle/ChestShopRestrictions/total)
![ChestShopRestrictions license](https://img.shields.io/github/license/kohle/ChestShopRestrictions)

ChestShopRestrictions is a [Paper](https://papermc.io/) plugin that adds limits for [ChestShop](https://github.com/ChestShop-authors/ChestShop-3) by Phoenix616.

View the [ChestShopRestrictions wiki](https://github.com/kohle/ChestShopRestrictions/wiki) for more information and configuration options.

### Features
- Set a global minimum price for buying and selling items.
- Set a global requirement to use whole numbers (no decimals) for prices.
- Enforce a maximum number of shops based on permissions.
- Configurable player messages using [MiniMessage format](https://docs.papermc.io/adventure/minimessage/) and placeholders.

### Configuration and Storage
* The plugin configuration is located at `/plugins/ChestShopRestrictions/config.yml`.
* Player shop counts and locations are tracked with SQLite (default) or MySQL.

### Compatibility
* Requires [ChestShop](https://github.com/ChestShop-authors/ChestShop-3) by Phoenix616.
* Requires an economy plugin compatible with Vault.
* Tested on Paper and Minecraft 1.21.11.

![ChestShopRestrictions statistics](https://bstats.org/signatures/bukkit/ChestShopRestrictions.svg)
