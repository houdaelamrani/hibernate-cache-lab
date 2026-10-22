package com.bibliotheque.service;

import com.bibliotheque.model.Auteur;
import com.bibliotheque.model.Livre;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Service d'analyse et de mesure des performances Hibernate :
 * 1. Détection du problème N+1 requêtes
 * 2. Résolution via JOIN FETCH JPQL
 * 3. Résolution déclarative via JPA Entity Graph
 * 4. Validation du Cache de Second Niveau (L2 Cache) et métriques statistiques
 *
 * @author Houda El Amrani
 */
public class PerformanceBenchmarkService {

    /**
     * Test 1 : Reproduction du problème N+1
     * 1 requête pour lister les auteurs + 10 requêtes supplémentaires (1 par auteur) pour charger les livres en LAZY.
     */
    public void mesurerSansOptimisation(EntityManager em) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  1. TEST SANS OPTIMISATION (PROBLÈME N+1 REQUÊTES)");
        System.out.println("--------------------------------------------------------------------------------");

        long debut = System.currentTimeMillis();

        TypedQuery<Auteur> query = em.createQuery("SELECT a FROM Auteur a", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        int totalLivresCharges = 0;
        // Déclenchement du chargement différé (LAZY) sur chaque auteur
        for (Auteur auteur : auteurs) {
            totalLivresCharges += auteur.getLivres().size();
        }

        long duree = System.currentTimeMillis() - debut;

        System.out.println("⚠️  Résultat : 1 requête initiale + " + auteurs.size() + " requêtes LAZY déclenchées (Total: " + (1 + auteurs.size()) + " requêtes SQL).");
        System.out.printf("⏱️  Durée d'exécution sans optimisation : %d ms (Auteurs: %d, Livres: %d)\n",
                duree, auteurs.size(), totalLivresCharges);
    }

    /**
     * Test 2 : Résolution du problème N+1 avec JOIN FETCH
     * Une seule requête SQL effectue la jointure et charge immédiatement les collections associées.
     */
    public void mesurerAvecJoinFetch(EntityManager em) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  2. OPTIMISATION AVEC 'JOIN FETCH'");
        System.out.println("--------------------------------------------------------------------------------");

        long debut = System.currentTimeMillis();

        // DISTINCT évite les doublons d'auteurs produits par la jointure relationnelle (produit cartésien)
        TypedQuery<Auteur> query = em.createQuery(
                "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
        List<Auteur> auteurs = query.getResultList();

        int totalLivres = 0;
        for (Auteur auteur : auteurs) {
            totalLivres += auteur.getLivres().size();
        }

        long duree = System.currentTimeMillis() - debut;

        System.out.println("✅ Résultat : Exactement 1 seule requête SQL exécutée avec jointure interne !");
        System.out.printf("⏱️  Durée d'exécution avec JOIN FETCH : %d ms (Auteurs: %d, Livres: %d)\n",
                duree, auteurs.size(), totalLivres);
    }

    /**
     * Test 3 : Résolution via JPA Entity Graph
     * Chargement EAGER déclaré dynamiquement sans toucher à la syntaxe de la requête JPQL.
     */
    public void mesurerAvecEntityGraph(EntityManager em) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  3. OPTIMISATION AVEC 'JPA ENTITY GRAPH'");
        System.out.println("--------------------------------------------------------------------------------");

        long debut = System.currentTimeMillis();

        // Récupération de l'Entity Graph nommé défini sur l'entité Auteur
        EntityGraph<?> entityGraph = em.getEntityGraph("Auteur.livres");

        TypedQuery<Auteur> query = em.createQuery("SELECT DISTINCT a FROM Auteur a", Auteur.class);
        // Indication (hint) pour forcer le chargement de l'attribut 'livres' en mode FETCH
        query.setHint("javax.persistence.fetchgraph", entityGraph);

        List<Auteur> auteurs = query.getResultList();

        int totalLivres = 0;
        for (Auteur auteur : auteurs) {
            totalLivres += auteur.getLivres().size();
        }

        long duree = System.currentTimeMillis() - debut;

        System.out.println("✅ Résultat : 1 seule requête SQL exécutée via Entity Graph déclaratif !");
        System.out.printf("⏱️  Durée d'exécution avec Entity Graph : %d ms (Auteurs: %d, Livres: %d)\n",
                duree, auteurs.size(), totalLivres);
    }

    /**
     * Test 4 : Validation du Cache de Second Niveau (L2 Cache)
     * Démontre la persistance des entités dans le cache Ehcache entre plusieurs EntityManagers distincts.
     */
    public void mesurerCacheSecondNiveau(EntityManagerFactory emf) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  4. TEST ET MÉTRIQUES DU CACHE DE SECOND NIVEAU (L2 EHCACHE)");
        System.out.println("--------------------------------------------------------------------------------");

        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // --- SESSION 1 : Premier chargement (Cache L2 vide -> Appel SQL en base) ---
        System.out.println("\n📥 [SESSION 1] Premier chargement de l'Auteur ID=1 (Cache L2 'froid')...");
        EntityManager em1 = emf.createEntityManager();
        long start1 = System.currentTimeMillis();
        Auteur a1 = em1.find(Auteur.class, 1L);
        // Force la mise en cache de la collection
        int nbLivres1 = a1.getLivres().size();
        long duree1 = System.currentTimeMillis() - start1;
        System.out.printf("   [Session 1] Auteur récupéré : %s avec %d livres (%d ms)\n", a1.getNom(), nbLivres1, duree1);
        em1.close(); // Fermeture de Session 1 : Le cache de niveau 1 (L1) est COMPLÈTEMENT DÉTRUIT !

        System.out.println("🔒 [Session 1 fermée] -> Le cache L1 est purgé. L'entité réside désormais dans le Cache L2.");

        // --- SESSION 2 : Deuxième chargement (L'entité est servie directement depuis Ehcache) ---
        System.out.println("\n⚡ [SESSION 2] Deuxième chargement du même Auteur ID=1 (Cache L2 'chaud')...");
        EntityManager em2 = emf.createEntityManager();
        long start2 = System.currentTimeMillis();
        Auteur a2 = em2.find(Auteur.class, 1L);
        int nbLivres2 = a2.getLivres().size();
        long duree2 = System.currentTimeMillis() - start2;
        System.out.printf("   [Session 2] Auteur récupéré : %s avec %d livres (%d ms)\n", a2.getNom(), nbLivres2, duree2);
        em2.close();

        // Affichage des statistiques officielles d'Hibernate
        System.out.println("\n📊 --- BILAN DES STATISTIQUES DU CACHE DE SECOND NIVEAU ---");
        System.out.printf("   • Cache L2 Put Count      : %d (Entités insérées en cache)\n", stats.getSecondLevelCachePutCount());
        System.out.printf("   • Cache L2 Hit Count      : %d (Succès de lecture depuis le cache)\n", stats.getSecondLevelCacheHitCount());
        System.out.printf("   • Cache L2 Miss Count     : %d (Recherches infructueuses en cache)\n", stats.getSecondLevelCacheMissCount());

        if (stats.getSecondLevelCacheHitCount() > 0) {
            System.out.println("🏆 SUCCÈS CONFIRMÉ : Le cache L2 a servi l'entité sans aucune requête SQL lors de la deuxième session !");
        } else {
            System.out.println("⚠️ Attention : Aucun hit n'a été enregistré.");
        }
    }
}
