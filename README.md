# Storage Units

Plugin **Paper / Purpur 1.21.11+** (Java 21) qui ajoute des coffres « Unités de stockage ».
Une unité ne stocke qu'**un seul type d'item** à la fois, mais en très grande quantité
(plusieurs millions possible). Compatible avec les joueurs **Java** et **Bedrock**
passant par Geyser/Floodgate, car :

* le bloc posé est un vrai `CHEST` vanilla, visible de la même manière des deux côtés ;
* l'interface graphique est une `Inventory` Bukkit standard, rendue nativement par les deux clients ;
* tous les textes utilisent **Adventure**, supporté identiquement par les deux pipelines.

---

## 📸 Aperçu visuel

### L'item « Unité de stockage » dans l'inventaire
![Item Unité de stockage](docs/screenshots/01-item-inventory.png)

### Le bloc coffre placé dans le monde
![Bloc coffre dans le monde](docs/screenshots/02-block-world.png)

### L'interface — unité vide (Niveau 1)
![Interface unité vide](docs/screenshots/03-gui-empty.png)

### L'interface — unité remplie (Niveau 3, 347 250 / 500 000 diamants)
![Interface unité remplie](docs/screenshots/04-gui-filled.png)

---

## ✨ Fonctionnalités

* 📦 Bloc coffre custom — Un seul type d'item à la fois
* 📈 Système d'**amélioration par niveau** (1 → 4+ configurable)
* 💾 Persistance YAML (un fichier par unité + un index)
* 🔌 API publique (pour d'autres plugins)
* ⚙️ 100 % configurable (capacités, items, sons, recettes, messages)
* 🔊 Sons Minecraft vanilla (configurables)
* 🧰 Recettes de craft configurables (YAML)

---

## 🏗️ Architecture

```
fr.souxch06.storageunits
├── api                       API publique (pour plugins tiers)
│   ├── StorageUnitsApi
│   └── StorageUnitSnapshot
├── bootstrap
│   └── StorageUnits          Classe principale (onEnable / onDisable)
├── commands
│   └── StorageUnitCommand    /su, /storageunits
├── config
│   ├── ConfigManager         config.yml
│   ├── LanguageManager       lang/*.yml
│   └── RecipeConfig          recipes.yml
├── data
│   └── StorageRepository     YAML par unité
├── gui
│   └── StorageGui            Inventaire 27 cases
├── listeners
│   ├── UnitBlockListener     Casse, piston, explosion
│   ├── UnitInteractionListener  Clic droit + GUI
│   └── UnitItemListener      Placement de l'item
├── manager
│   └── StorageManager        Logique métier
├── model
│   ├── StorageLevel          Niveau d'unité (capacité)
│   └── StorageUnit           Modèle de l'unité
└── util
    ├── ItemUtil              Couleurs Adventure, names
    ├── PluginKeys            NamespacedKeys
    └── UnitItemFactory       Fabrique d'item
```

---

## 🚀 Compilation

```bash
mvn clean package
```

Le jar prêt à l'emploi est dans `target/StorageUnits-1.0.0.jar`.

---

## 📥 Installation

1. Copier le jar dans `plugins/` de votre serveur **Paper 1.21.11+** ou **Purpur 1.21.11+**.
2. Redémarrer le serveur.
3. Les fichiers `config.yml`, `lang/fr.yml`, `recipes.yml` sont générés dans `plugins/StorageUnits/`.

---

## 🎮 Utilisation

| Commande | Description | Permission |
|---|---|---|
| `/su give` | Donne une unité au joueur courant. | `storageunits.give` |
| `/su give <joueur> [niveau]` | Donne une unité à un joueur. | `storageunits.give` |
| `/su status` | Affiche le statut global. | `storageunits.use` |
| `/su list [page]` | Liste les unités chargées. | `storageunits.use` |
| `/su reload` | Recharge la configuration. | `storageunits.admin` |

**Placez** ensuite l'item coffre reçu. Un coffre vanilla est posé et marqué
via le PDC. **Clic droit** sur le coffre pour ouvrir l'interface :

* 🟩 Déposer : vide l'inventaire du joueur des items compatibles dans l'unité
* 🟥 Retirer : retire 1 stack (ou tout avec Shift)
* 🟪 Améliorer : passe au niveau supérieur (si configuré)

---

## ⚙️ Configuration (`config.yml`)

```yaml
settings:
  language: "fr"
  default-level: 1
  max-stacks-per-click: 2304

levels:
  1: { capacity: 100000,  display-name: "Niveau 1" }
  2: { capacity: 250000,  display-name: "Niveau 2" }
  3: { capacity: 500000,  display-name: "Niveau 3" }
  4: { capacity: 1000000, display-name: "Niveau 4" }
  5: { capacity: 5000000, display-name: "Niveau 5 (Légendaire)" }

unit:
  material: CHEST
  name: "&6&lUnité de stockage"
  lore:
    - "&7Stocke une très grande quantité"
    - "&7d'un seul type d'objet."
  glowing: true
  custom-model-data: 0

sounds:
  open: BLOCK_CHEST_OPEN
  close: BLOCK_CHEST_CLOSE
  deposit: ENTITY_ITEM_PICKUP
  withdraw: ENTITY_ITEM_PICKUP
  upgrade: ENTITY_PLAYER_LEVELUP
```

### Ajouter un niveau

Il suffit d'ajouter une entrée dans `levels:` — **aucune modification de code n'est nécessaire** :

```yaml
levels:
  6:
    capacity: 25000000
    display-name: "Niveau 6 (Divin)"
```

### Recettes (`recipes.yml`)

```yaml
recipes:
  basic_chest:
    enabled: true
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

## 🔌 API publique

```java
import fr.souxch06.storageunits.api.StorageUnitsApi;
import fr.souxch06.storageunits.api.StorageUnitSnapshot;

StorageUnitsApi api = StorageUnitsApi.get();
if (api != null) {
    // Créer une unité à un emplacement
    java.util.UUID id = api.createUnit(loc, 1, player.getUniqueId());

    // Déposer / retirer
    api.deposit(id, itemStack);
    api.withdraw(id, player, 64);

    // Lister
    for (StorageUnitSnapshot s : api.listAll()) {
        plugin.getLogger().info("Unité " + s.id() + " -> " + s.amount());
    }
}
```

---

## 💾 Persistance

Les unités sont stockées dans `plugins/StorageUnits/units/<uuid>.yml` :

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

L'index `units/index.yml` accélère la recherche par emplacement.

---

## 🌐 Compatibilité

* **Serveur** : Paper 1.21.11+ / Purpur 1.21.11+ (API Bukkit + Paper + Adventure)
* **Java** : 21
* **Clients** : Minecraft Java Edition **et** Minecraft Bedrock (Geyser + Floodgate)
* Les upgrades du plugin ciblent l'API moderne ; le code évite les méthodes
  dépréciées de Bukkit et utilise Adventure pour le texte.

---

## 🛠️ Extensibilité

* **Nouveau comportement** : créez une classe dans `manager/` et appelez-la
  depuis le listener / GUI existant.
* **Nouvelles clés de config** : ajoutez-les à `ConfigManager` (lecture et cache).
* **Nouvelles traductions** : copiez `lang/fr.yml` → `lang/xx.yml` et traduisez.
* **Nouvelles recettes** : ajoutez une entrée dans `recipes.yml`.

---

## 📜 Licence

MIT © Souxch06
