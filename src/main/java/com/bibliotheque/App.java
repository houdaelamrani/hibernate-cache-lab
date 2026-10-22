package com.bibliotheque;

import com.bibliotheque.service.DataInitService;
import com.bibliotheque.service.PerformanceBenchmarkService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Classe principale orchestrant l'exécution du TP :
 * - Initialisation des données
 * - Démonstration du problème N+1
 * - Comparaison JOIN FETCH vs Entity Graph
 * - Validation et métriques du Cache L2 Ehcache
 *
 * @author Houda El Amrani
 */
public class App {

    public static void main(String[] args) {
        afficherBanniere();

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        EntityManager em = emf.createEntityManager();

        try {
            DataInitService dataInitService = new DataInitService();
            PerformanceBenchmarkService benchmarkService = new PerformanceBenchmarkService();

            // Étape 1 : Initialisation du jeu de données (10 auteurs, 50 livres)
            System.out.println("\n📦 [ÉTAPE 1] INITIALISATION DE LA BASE DE DONNÉES EN MÉMOIRE (H2)");
            dataInitService.initData(em);

            // Étape 2 : Test sans optimisation (observation du N+1)
            benchmarkService.mesurerSansOptimisation(em);

            // Étape 3 : Résolution via JOIN FETCH
            benchmarkService.mesurerAvecJoinFetch(em);

            // Étape 4 : Résolution déclarative via JPA Entity Graph
            benchmarkService.mesurerAvecEntityGraph(em);

            // Étape 5 : Test multi-session du Cache de Second Niveau (L2 Cache)
            benchmarkService.mesurerCacheSecondNiveau(emf);

            System.out.println("\n================================================================================");
            System.out.println("  FIN DE L'EXÉCUTION DU TP - TOUTES LES OPTIMISATIONS SONT VALIDÉES !");
            System.out.println("================================================================================");

        } finally {
            if (em.isOpen()) {
                em.close();
            }
            if (emf.isOpen()) {
                emf.close();
            }
        }
    }

    private static void afficherBanniere() {
        System.out.println("================================================================================");
        System.out.println("  TP JPA & HIBERNATE : CACHE DE SECOND NIVEAU & RÉSOLUTION DU N+1");
        System.out.println("  Application : Bibliothèque Numérique & Maison d'Édition");
        System.out.println("  Auteure     : Houda El Amrani");
        System.out.println("================================================================================");
    }
}
