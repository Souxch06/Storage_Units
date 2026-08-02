# Politique de sécurité — Storage Units

## 🛡️ Versions supportées

| Version | Supportée          |
|---------|--------------------|
| 1.0.x   | ✅ Active          |
| < 1.0   | ❌ Plus de support |

## 🚨 Signaler une vulnérabilité

**Ne créez PAS d'issue publique** pour signaler un problème de sécurité.

À la place :

1. **Email** : ouvrez un ticket privé via l'onglet **Security** du repo
   GitHub (https://github.com/Souxch06/Storage_Units/security/advisories/new)
2. **Ou** envoyez un email direct à l'auteur (cf. son profil GitHub)

Vous recevrez une réponse sous **72 h** et un correctif sous **14 jours**
pour les failles critiques.

## 🔒 Bonnes pratiques déployées

Le projet est protégé en plusieurs couches :

### Côté dépôt GitHub
* **Protection de branche `main`** : pas de push direct, PR obligatoire
* **Revue obligatoire par CODEOWNERS** : tout code Java doit être validé
  par @Souxch06
* **Détection de secrets** : activée par défaut par GitHub (push
  protection contre les commits de clés AWS, tokens, etc.)
* **Historique immutable** : force push désactivé sur `main`

### Côté code
* **Pas de dépendances inutiles** : seul `paper-api` + `snakeyaml`
  (fourni par Bukkit)
* **API Adventure** au lieu de `String` legacy pour le texte :
  immunisé contre les injections de format
* **PersistentDataContainer** au lieu de NBT : pas d'accès au NMS
  instable, les données sont versionnées par Paper
* **Permissions vérifiées** côté serveur (storageunits.use,
  storageunits.give, storageunits.admin, storageunits.upgrade)
* **Pas d'évaluation dynamique** : aucun `eval` ou chargement de
  classes arbitraires
* **YAML désérialisé via SnakeYAML en mode safe** : pas de
  désérialisation Java arbitraire (résistante à la CVE-2022-1471)

### Côté serveur (recommandations pour les admins)
* Le plugin n'exécute **aucune commande shell**
* Le plugin **n'ouvre aucun port réseau**
* Le plugin **n'écrit que dans son propre dossier** (`plugins/StorageUnits/`)
* En cas de suspicion, regarder `plugins/StorageUnits/units/` : un
  fichier par unité, facile à auditer

## 🔐 Modèle de menace

Le plugin est conçu pour des serveurs de communauté (Java + Bedrock).
Il suppose :
* Le propriétaire du serveur est de confiance (sinon il pourrait
  exécuter n'importe quel plugin)
* Les fichiers `config.yml`, `recipes.yml` et `lang/*.yml` sont
  édités par un admin
* Le système de fichiers du serveur est protégé (droits Unix classiques)

Il n'a pas besoin d'élévation de privilèges : il s'exécute avec les
droits d'un plugin Bukkit standard.

## 📜 Changelog sécurité

| Date       | Version | Note |
|------------|---------|------|
| 2026-08-02 | 1.0.0   | Première release publique |
