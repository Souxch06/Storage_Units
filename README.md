# Storage Units

A **Paper / Purpur 1.21.11+** plugin (Java 21) that adds "Storage Unit" chests.
A unit stores only **a single item type** at a time, but in very large quantities
(several million possible). Compatible with both **Java** and **Bedrock** players
connecting through Geyser/Floodgate, because:

* the placed block is a real vanilla `CHEST`, displayed identically on both sides;
* the graphical interface is a standard Bukkit `Inventory`, rendered natively by both clients;
* all text uses **Adventure**, supported identically by both pipelines.

## 📥 Download

**Latest stable release: v1.0.0**

- 🔗 **Releases page**: https://github.com/Souxch06/Storage_Units/releases
- 📦 **Direct JAR download**: [StorageUnits-1.0.0.jar](https://github.com/Souxch06/Storage_Units/releases/download/v1.0.0/StorageUnits-1.0.0.jar)

```bash
wget https://github.com/Souxch06/Storage_Units/releases/download/v1.0.0/StorageUnits-1.0.0.jar -O plugins/StorageUnits-1.0.0.jar
```

> ✅ JAR automatically built via GitHub Actions (Java 21, Paper API 1.21.11+). Always make sure you download from the official releases page.

---

## 📸 Visual preview

### The "Storage Unit" item in the inventory
![Storage Unit item](docs/screenshots/01-item-inventory.png)

### The chest block placed in the world
![Chest block in the world](docs/screenshots/02-block-world.png)

### The interface — empty unit (Level 1)
![Empty unit interface](docs/screenshots/03-gui-empty.png)

### The interface — filled unit
![Filled unit interface](docs/screenshots/04-gui-filled.png)

---

## ✨ Features

* 📦 Custom chest block — only one item type at a time
* 📈 **Level-based upgrade** system (1 → 5+ configurable)
* 💾 YAML persistence (one file per unit + an index)
* 🔌 Public API (for other plugins)
* ⚙️ 100 % configurable (capacities, items, sounds, recipes, messages)
* 🔊 Vanilla Minecraft sounds (configurable)
* 🧰 Configurable crafting recipes (YAML) — **disabled by default**
* 🪪 Clearly identifiable item in the inventory (name "✦ Storage Unit" + lore + glow + PDC tag) — **distinct from a vanilla chest**
* 🌐 Placed block = vanilla chest, so **identical on Java & Bedrock**

---

## 🏗️ Architecture

```
fr.souxch06.storageunits
├── api                       Public API (for third-party plugins)
│   ├── StorageUnitsApi
│   └── StorageUnitSnapshot
├── bootstrap
│   └── StorageUnits          Main class (onEnable / onDisable)
├── commands
│   └── StorageUnitCommand    /su, /storageunits
├── config
│   ├── ConfigManager         config.yml
│   ├── LanguageManager       lang/*.yml
│   └── RecipeConfig          recipes.yml
├── data
│   └── StorageRepository     YAML per unit
├── gui
│   └── StorageGui            27-slot inventory
├── listeners
│   ├── UnitBlockListener     Breaking, piston, explosion
│   ├── UnitInteractionListener  Right-click + GUI
│   └── UnitItemListener      Placing the item
├── manager
│   └── StorageManager        Business logic
├── model
│   ├── StorageLevel          Unit level (capacity)
│   └── StorageUnit           Unit model
└── util
    ├── ItemUtil              Adventure colors, names
    ├── PluginKeys            NamespacedKeys
    └── UnitItemFactory       Item factory
```

---

## 🚀 Building

```bash
mvn clean package
```

The ready-to-use jar is in `target/StorageUnits-1.0.0.jar`.

> 💡 You can also grab the prebuilt JAR from the [latest release](https://github.com/Souxch06/Storage_Units/releases/latest):
> https://github.com/Souxch06/Storage_Units/releases/download/v1.0.0/StorageUnits-1.0.0.jar

---

## 📥 Installation

1. Copy the jar into the `plugins/` folder of your **Paper 1.21.11+** or **Purpur 1.21.11+** server.
2. Restart the server.
3. The files `config.yml`, `lang/fr.yml`, `recipes.yml` are generated in `plugins/StorageUnits/`.

### Updating

1. Stop the server.
2. Replace the existing jar in `plugins/` with the new version.
3. Restart the server.

> ⚠️ **Do not delete `plugins/StorageUnits/` while updating.** This folder holds
> your configuration as well as the persistent data of your units in `units/`.

---

## 🎮 Usage

| Command | Description | Permission |
|---|---|---|
| `/su give` | Gives a unit to the current player. | `storageunits.give` |
| `/su give <player> [level]` | Gives a unit to a player. | `storageunits.give` |
| `/su status` | Shows the global status. | `storageunits.use` |
| `/su list [page]` | Lists the loaded units. | `storageunits.use` |
| `/su craft` | Shows the crafting system status. | `storageunits.use` |
| `/su reload` | Reloads the configuration. | `storageunits.admin` |

Then **place** the chest item you received. A vanilla chest is placed and tagged
through the PDC. **Right-click** the chest to open the interface:

* 🟩 Deposit: empties the player's inventory of compatible items into the unit
* 🟥 Withdraw: takes out 1 stack (or everything with Shift)
* 🟪 Upgrade: moves to the next level (if configured)

> 🪪 **How to tell a unit apart from a vanilla chest?**
> The "✦ Storage Unit" item stands out thanks to:
> - its **bold golden name** with the `✦` prefix
> - its **lore** mentioning "Storage Units" and the capacity
> - the **glowing effect** (invisible enchantment)
> - the **PDC tag** `storageunits:unit_item` (server side)
> The placed block remains a vanilla chest, to stay visually identical for both Java and Bedrock.

---

## ⚙️ Configuration (`config.yml`)

```yaml
settings:
  language: "en"
  default-level: 1
  max-stacks-per-click: 2304

levels:
  1: { capacity: 512,    display-name: "Level 1" }
  2: { capacity: 2048,   display-name: "Level 2" }
  3: { capacity: 8192,   display-name: "Level 3" }
  4: { capacity: 32768,  display-name: "Level 4" }
  5: { capacity: 131072, display-name: "Level 5" }

unit:
  material: CHEST
  name: "&6&lStorage Unit"
  lore:
    - "&7Stores a very large quantity"
    - "&7of a single item type."
  glowing: true
  custom-model-data: 0

sounds:
  open: BLOCK_CHEST_OPEN
  close: BLOCK_CHEST_CLOSE
  deposit: ENTITY_ITEM_PICKUP
  withdraw: ENTITY_ITEM_PICKUP
  upgrade: ENTITY_PLAYER_LEVELUP
```

### Adding a level

Simply add an entry under `levels:` — **no code change is required**:

```yaml
levels:
  6:
    capacity: 25000000
    display-name: "Level 6 (Divine)"
```

### Recipes (`recipes.yml`)

**⚠️ Crafting is DISABLED BY DEFAULT** (config.yml → `craft.enabled: false`
and every recipe in `recipes.yml` is also set to `enabled: false`).
Developers / admins are free to enable / disable / create whatever
recipes they want.

To enable crafting:

1. In `config.yml`: set `craft.enabled` to `true`.
2. In `recipes.yml`: set the desired recipe to `enabled: true`.
3. Run `/su reload` (or restart the server).

```yaml
# config.yml
craft:
  enabled: true   # <- set to true

# recipes.yml
recipes:
  basic_chest:
    enabled: true   # <- set to true
    level: 1
    amount: 1
    shape:
      - "III"
      - "ICI"
      - "III"
    ingredients:
      I: IRON_INGOT
      C: CHEST
```

---

## 🔌 Public API

```java
import fr.souxch06.storageunits.api.StorageUnitsApi;
import fr.souxch06.storageunits.api.StorageUnitSnapshot;

StorageUnitsApi api = StorageUnitsApi.get();
if (api != null) {
    // Create a unit at a location
    java.util.UUID id = api.createUnit(loc, 1, player.getUniqueId());

    // Deposit / withdraw
    api.deposit(id, itemStack);
    api.withdraw(id, player, 64);

    // List
    for (StorageUnitSnapshot s : api.listAll()) {
        plugin.getLogger().info("Unit " + s.id() + " -> " + s.amount());
    }
}
```

---

## 💾 Persistence

Units are stored in `plugins/StorageUnits/units/<uuid>.yml`:

```yaml
id: 1a2b3c4d-...
owner: 9f8e7d6c-...
level: 2
amount: 12345
world: world
x: 10
y: 64
z: -20
material: DIAMOND
template: {...}
```

The index `units/index.yml` speeds up lookups by location.

---

## 🌐 Compatibility

* **Server**: Paper 1.21.11+ / Purpur 1.21.11+ (Bukkit + Paper + Adventure API)
* **Java**: 21
* **Clients**: Minecraft Java Edition **and** Minecraft Bedrock (Geyser + Floodgate)
* The plugin's upgrades target the modern API; the code avoids deprecated
  Bukkit methods and uses Adventure for text.

---

## 🛡️ Security & contributing

* 📋 [`CONTRIBUTING.md`](CONTRIBUTING.md) — guide for submitting a PR
* 🔒 [`SECURITY.md`](SECURITY.md) — report a vulnerability
* 🪪 [`CODEOWNERS`](CODEOWNERS) — who must approve changes
* ⚙️ [`.github/workflows/ci.yml`](.github/workflows/ci.yml) — automatic Maven CI
* 🛡️ [`docs/SECURITY_SETUP.md`](docs/SECURITY_SETUP.md) — checklist to enable
  branch protection on GitHub (takes about 1 minute)
* 📜 [`LICENSE`](LICENSE) — explicit MIT license

## 🛠️ Extensibility

* **New behavior**: create a class in `manager/` and call it from the existing
  listener / GUI.
* **New config keys**: add them to `ConfigManager` (read and cache).
* **New translations**: copy `lang/fr.yml` → `lang/xx.yml` and translate it.
* **New recipes**: add an entry in `recipes.yml`.

---

## 💝 Supporting the project

If **Storage Units** is useful to you and you'd like to support its
development, you can make a donation via PayPal:

<p align="center">
  <img src="docs/donations/paypal-qrcode.png" alt="PayPal QR code to make a donation" width="280" />
</p>

<p align="center">
  <em>Scan the QR code with the PayPal app to make a donation.</em>
</p>

Donations are **100 % optional** and grant no in-game advantage.
They are solely used to support maintenance and the addition of new
features. Thanks to all contributors and donors! 🙏

---

## 👥 Credits

* **Author**: [Souxch06](https://github.com/Souxch06)
* **Development assistance**: **Arena Agent** (MiniMax-M3), the AI agent of the [Arena.ai](https://arena.ai) platform — architecture design, code writing, documentation, preview screenshots, and advice on the Paper / Adventure API.
* **Technologies**:
  * [Paper API 1.21.11+](https://papermc.io/) — modern Bukkit/Spigot backend
  * [Adventure](https://docs.advntr.dev/) — Minecraft's text engine (colors, components)
  * [Geyser + Floodgate](https://geysermc.org/) — Java ↔ Bedrock bridge

Suggestions, code, and documentation were produced through human + AI
collaboration. Feel free to open an issue / PR for any improvement.

---

## 📜 License

MIT © Souxch06
