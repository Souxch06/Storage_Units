# Documentation

Ce dossier regroupe la documentation et les supports visuels du projet.

## 📂 Structure

```
docs/
├── README.md                  Ce fichier
└── screenshots/              Captures d'écran du plugin en action
    ├── 01-item-inventory.png  Item « Unité de stockage » dans la hotbar
    ├── 02-block-world.png     Bloc coffre placé dans le monde
    ├── 03-gui-empty.png       Interface, unité vide (niveau 1)
    └── 04-gui-filled.png      Interface, unité remplie (niveau 3, diamants)
```

## 🖼️ À propos des captures d'écran

Les captures sont des **rendus générés par IA** (cohérents avec le look vanilla
Minecraft 1.21+) servant d'illustration. Elles reflètent fidèlement :

* le nom, le lore et l'effet glow de l'item ;
* le modèle vanilla du coffre posé ;
* la mise en page de l'interface (27 cases, icône, infos, boutons Déposer / Retirer / Améliorer).

Les valeurs exactes (quantités, capacités, niveaux) sont définies dans `src/main/resources/config.yml`.
Les textes visibles à l'écran sont définis dans `src/main/resources/lang/fr.yml`.

## 🔄 Régénérer les captures

Les captures originales ont été générées par un modèle d'image. Pour les mettre
à jour après un changement visuel (nouveau nom, nouveau lore, nouveau layout) :

1. Adapter le prompt avec les nouveaux textes / positions de slots.
2. Générer de nouvelles images (par exemple via l'outil de génération d'images
   intégré à Arena).
3. Remplacer les fichiers dans `docs/screenshots/` en conservant les **mêmes
   noms** pour que les liens dans `README.md` continuent de fonctionner.
4. Commiter et pousser.

> 💡 Astuce : pour réduire le poids du dépôt, lancez avant le commit :
>
> ```bash
> mogrify -strip -define png:compression-level=9 docs/screenshots/*.png
> ```
