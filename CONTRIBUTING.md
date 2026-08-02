# Guide de contribution — Storage Units

Merci de votre intérêt pour **Storage Units** ! 🎉

## 🚀 Comment contribuer

### 1. Signaler un bug

* Vérifiez d'abord qu'il n'y a pas déjà une issue ouverte
* Utilisez le template "Bug report" et incluez :
  * Version du plugin (`/su status` ou fichier `plugin.yml`)
  * Version de Paper / Purpur
  * Version Java
  * Logs serveur (extrait pertinent, pas 1000 lignes)
  * Étapes de reproduction

### 2. Proposer une fonctionnalité

* Ouvrez une **issue** avec le tag `enhancement`
* Décrivez le cas d'usage (pas la solution technique)
* Si c'est une grosse feature, attendez l'accord avant de coder

### 3. Soumettre une Pull Request

1. **Fork** le repo
2. Créez une branche : `git checkout -b fix/ma-correction`
3. Commitez avec un message clair :
   * `fix: ...` pour un bug
   * `feat: ...` pour une feature
   * `docs: ...` pour la doc
   * `refactor: ...` pour du nettoyage
4. **Poussez** et ouvrez une PR
5. La CI (si activée) va tourner automatiquement
6. **Attendez la revue** de @Souxch06 (CODEOWNERS)
7. Squash & merge une fois approuvé

## 📋 Standards de code

* **Java 21+** : on utilise les records, `var`, les switch expressions
* **API moderne** : Adventure (`Component` / `LegacyComponentSerializer`)
  au lieu de `String` legacy, `getItem()` au lieu de `getItemInHand()`
* **Pas de NMS** : uniquement l'API publique Paper / Bukkit
* **Pas de dépendances supplémentaires** : `paper-api` est la seule
  dépendance runtime
* **Javadoc en français** : pour rester cohérent avec le reste du projet
* **Imports propres** : pas d'imports inutilisés (vérifié en CI)

## 🧪 Tests

* Tests unitaires JUnit 5 dans `src/test/java/`
* Lancez-les avec : `mvn test`
* Toute nouvelle logique métier doit avoir un test couvrant :
  * Le cas nominal
  * Le cas limite (vide, plein, null)
  * Le cas d'erreur (input invalide)

## 📦 Structure d'une PR

```
Titre: feat: add support for iron_ingot in level 3 recipes

Description:
- Ajoute la recette `recipes.yml` permettant de crafter une unité
  de niveau 3 avec 8 lingots de fer
- Met à jour le README pour lister cette nouvelle recette
- Ajoute un test unitaire pour la fonction de validation

Tests:
- [x] mvn test passe
- [x] Test manuel sur Paper 1.21.11 OK
- [x] Compatible Bedrock (Geyser)

Captures d'écran:
(ajouter si pertinent)
```

## 📜 Licence

En contribuant, vous acceptez que vos contributions soient sous
licence **MIT** (même licence que le projet).
