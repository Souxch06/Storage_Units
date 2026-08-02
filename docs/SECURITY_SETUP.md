# 🔒 Guide de protection du repo

Ce document explique comment activer **en 1 minute** la protection
complète du repo `Souxch06/Storage_Units` une fois que l'agent (moi)
aura commité tous les fichiers nécessaires.

## ✅ Étape 1 : Activer la protection de branche

Va sur :
**https://github.com/Souxch06/Storage_Units/settings/branches**

1. Clique sur **Add rule** (ou **Add classic protection rule**)
2. **Branch name pattern** : `main`
3. Coche :
   - ☑️ **Require a pull request before merging**
     - ☑️ **Require approvals** : mets `1`
     - ☑️ **Dismiss stale pull request approvals when new commits are pushed**
     - ☑️ **Require review from Code Owners** ← grâce au fichier `CODEOWNERS`
     - ☐ Require approval of the most recent reviewable push (optionnel)
   - ☑️ **Require status checks to pass before merging** (si la CI GitHub
     Actions est activée, le job `build` sera listé — sinon ignore)
   - ☑️ **Require conversation resolution before merging**
   - ☑️ **Require signed commits** (optionnel mais recommandé)
   - ☑️ **Require linear history** (empêche les merge commits)
   - ☑️ **Include administrators** (les admins aussi doivent suivre la règle)
4. **Allow force pushes** : ❌ décoché
5. **Allow deletions** : ❌ décoché
6. Clique sur **Create** / **Save changes**

## ✅ Étape 2 : Activer la détection de secrets

Va sur :
**https://github.com/Souxch06/Storage_Units/settings/security_analysis**

Coche :
- ☑️ **Enable Dependabot alerts**
- ☑️ **Enable Dependabot security updates**
- ☑️ **Enable secret scanning**
- ☑️ **Enable push protection** (empêche le push si un secret est détecté)

## ✅ Étape 3 (optionnel) : Protéger aussi la branche de dev

Si tu utilises une branche `develop` ou autre, applique la même
procédure.

## ✅ Étape 4 : Activer GitHub Actions

Le fichier `.github/workflows/ci.yml` est déjà commité. Pour
l'activer :

Va sur **https://github.com/Souxch06/Storage_Units/actions**
et accepte la popup « enable workflows ».

## 🧪 Tester la protection

1. Crée une nouvelle branche : `git checkout -b test/protection`
2. Push un changement bidon
3. Ouvre une PR sur `main`
4. **Tu devrais voir** :
   - Le badge "Changes requested" ou "Review required"
   - Le job CI qui tourne (`build`)
   - L'obligation d'avoir l'approbation de @Souxch06
   - Pas de bouton "Merge" tant que les checks ne sont pas verts

## 🎯 Résultat final

Une fois tout activé :

| Protection | Statut |
|------------|--------|
| Push direct sur main | ❌ bloqué |
| Force push | ❌ bloqué |
| Suppression de main | ❌ bloquée |
| Merge sans approbation | ❌ bloqué |
| Merge sans CI verte | ❌ bloqué (optionnel) |
| Merge avec conversation ouverte | ❌ bloqué |
| Commit d'un secret (token AWS, etc.) | ❌ bloqué à la push |
| Code non revu par @Souxch06 | ❌ impossible à merger |

C'est la configuration **gold standard** pour un projet public
sur GitHub.
