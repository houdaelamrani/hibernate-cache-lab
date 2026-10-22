# TP JPA & Hibernate : Cache de Second Niveau (Ehcache 3) et Résolution du Problème N+1

> **Application :** Bibliothèque Numérique & Maison d'Édition  
> **Auteure :** Houda El Amrani  
> **Technologies :** Java, JPA 2.2, Hibernate 5.6, Ehcache 3.10 (JSR-107 JCache), Base H2 In-Memory, Maven  

---

## 🎯 Objectifs d'apprentissage

1. **Identifier et diagnostiquer le problème N+1 requêtes** généré par le chargement différé (*LAZY Loading*) des associations JPA.
2. **Éliminer le N+1 via la clause JPQL `JOIN FETCH`** en chargeant les entités et leurs collections en une requête SQL unique.
3. **Mettre en œuvre les JPA Entity Graphs (`@NamedEntityGraph`)** pour contrôler le chargement de manière déclarative et dynamique sans modifier les requêtes JPQL.
4. **Configurer et activer le Cache de Second Niveau (*L2 Cache*) avec Ehcache 3 (JCache / JSR-107)** pour partager les entités et collections entre plusieurs sessions distinctes.
5. **Comprendre le piège du cache des collections :** Découvrir pourquoi `@Cache` doit être annoté à la fois sur l'entité et sur le champ de la collection (`List<Livre>`).
6. **Analyser les métriques de performance et les statistiques officielles Hibernate** (`Hits`, `Misses`, `Puts`).

---

## 🏛️ Modèle Relationnel

```text
+------------------------------------+          +------------------------------------+
|               Auteur               | 1      * |               Livre                |
+------------------------------------+----------+------------------------------------+
| - id : Long [PK]                   |          | - id : Long [PK]                   |
| - nom : String                     |          | - titre : String                   |
| - nationalite : String             |          | - isbn : String                    |
| - biographie : String              |          | - anneePublication : int           |
| - livres : List<Livre> [@Cache]    |          | - prix : double                    |
+------------------------------------+          | - auteur : Auteur [FK, LAZY]       |
                                                +------------------------------------+
```

---

## 🔍 Analyse Comparative : Les 3 Approches face au Problème N+1

| Approche | Requêtes SQL exécutées | Doublons possibles ? | Modification du JPQL requise ? | Idéal pour... |
|---|---|---|---|---|
| **Sans Optimisation (N+1)** | **1 + N** *(11 requêtes pour 10 auteurs)* | Non | Non | À proscrire en production |
| **`JOIN FETCH`** | **1 seule requête** | Oui *(résolu avec `DISTINCT`)* | Oui | Requêtes spécifiques rapides |
| **`JPA Entity Graph`** | **1 seule requête** | Géré proprement | **Non** *(via hint déclaratif)* | Architecture propre & réutilisable |

---

## ⚡ Architecture des Niveaux de Cache dans Hibernate

```mermaid
graph TD
    subgraph "Application JEE"
        EM1[EntityManager 1 / Session 1]
        EM2[EntityManager 2 / Session 2]
    end

    subgraph "Niveau 1 : Cache de Session (L1)"
        L1_1["Cache L1 (Local à EM1 - Automatique)"]
        L1_2["Cache L1 (Local à EM2 - Automatique)"]
    end

    subgraph "Niveau 2 : Cache Partagé (L2 Ehcache 3)"
        L2["Cache L2 Partagé (Régions Auteur, Livre, Auteur.livres)"]
    end

    subgraph "Base de Données"
        DB[(Base de Données H2)]
    end

    EM1 --> L1_1
    EM2 --> L1_2

    L1_1 -- Miss L1 --> L2
    L1_2 -- Miss L1 --> L2

    L2 -- Hit L2 (Zéro SQL !) --> L1_2
    L2 -- Miss L2 (Premier accès) --> DB
```

---

## 🔄 Déroulement Temporel du Cache L2 entre Sessions

```mermaid
sequenceDiagram
    autonumber
    actor App as Application
    participant EM1 as Session 1 (EntityManager)
    participant EM2 as Session 2 (EntityManager)
    participant L2 as Cache L2 (Ehcache)
    participant DB as Base de Données H2

    Note over L2: État Initial : Cache L2 vide (froid)

    App->>EM1: em1.find(Auteur.class, 1L)
    EM1->>L2: Recherche Auteur ID=1
    Note over L2: ❌ Miss L2
    EM1->>DB: SELECT * FROM auteurs WHERE id = 1
    DB-->>EM1: Données Auteur (Tahar Ben Jelloun)
    EM1->>L2: Stockage dans le Cache L2 (Put Count = 1)
    App->>EM1: em1.close()
    Note over EM1: 💥 Cache L1 détruit !

    Note over App: Nouvelle transaction indépendante
    App->>EM2: em2.find(Auteur.class, 1L)
    EM2->>L2: Recherche Auteur ID=1
    Note over L2: ✅ Hit L2 (Données trouvées en mémoire !)
    L2-->>EM2: Retourne l'entité sans appel SQL
    Note over DB: 🛑 AUCUNE REQUÊTE SQL EXÉCUTÉE !
    App->>EM2: em2.close()
```

---

## 🚀 Guide d'Exécution

### 1. Lancer la Suite de Tests Unitaires JUnit 4
```powershell
mvn test
```

### 2. Lancer la Démonstration Complète du TP
```powershell
mvn compile exec:java
```

Ou double-cliquez directement sur [`run.bat`](run.bat).
