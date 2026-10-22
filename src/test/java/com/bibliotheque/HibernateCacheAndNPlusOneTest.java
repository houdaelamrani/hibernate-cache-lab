package com.bibliotheque;

import com.bibliotheque.model.Auteur;
import com.bibliotheque.service.DataInitService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Suite de tests unitaires automatisés validant :
 * 1. L'élimination du problème N+1 via JOIN FETCH
 * 2. L'élimination du N+1 via JPA Entity Graph
 * 3. L'efficacité du Cache de Second Niveau (L2 Cache Ehcache) à travers plusieurs sessions
 *
 * @author Houda El Amrani
 */
public class HibernateCacheAndNPlusOneTest {

    private static EntityManagerFactory emf;

    @BeforeClass
    public static void setUpClass() {
        emf = Persistence.createEntityManagerFactory("hibernate-cache-pu");
        EntityManager em = emf.createEntityManager();
        DataInitService initService = new DataInitService();
        initService.initData(em);
        em.close();
    }

    @AfterClass
    public static void tearDownClass() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    public void testJoinFetchChargeEnUneSeuleRequete() {
        EntityManager em = emf.createEntityManager();
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        try {
            TypedQuery<Auteur> query = em.createQuery(
                    "SELECT DISTINCT a FROM Auteur a JOIN FETCH a.livres", Auteur.class);
            List<Auteur> auteurs = query.getResultList();

            assertNotNull(auteurs);
            assertEquals("Il doit y avoir 10 auteurs", 10, auteurs.size());

            // Accéder à la collection ne doit déclencher aucune requête SQL supplémentaire
            long queryCountAvant = stats.getQueryExecutionCount();
            for (Auteur auteur : auteurs) {
                assertEquals(5, auteur.getLivres().size());
            }
            long queryCountApres = stats.getQueryExecutionCount();

            assertEquals("JOIN FETCH ne doit exécuter aucune requête additionnelle lors de l'accès aux collections",
                    queryCountAvant, queryCountApres);

        } finally {
            em.close();
        }
    }

    @Test
    public void testEntityGraphChargeEnUneSeuleRequete() {
        EntityManager em = emf.createEntityManager();
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        try {
            EntityGraph<?> entityGraph = em.getEntityGraph("Auteur.livres");
            TypedQuery<Auteur> query = em.createQuery("SELECT DISTINCT a FROM Auteur a", Auteur.class);
            query.setHint("javax.persistence.fetchgraph", entityGraph);

            List<Auteur> auteurs = query.getResultList();

            assertNotNull(auteurs);
            assertEquals(10, auteurs.size());

            long queryCountAvant = stats.getQueryExecutionCount();
            for (Auteur auteur : auteurs) {
                assertEquals(5, auteur.getLivres().size());
            }
            long queryCountApres = stats.getQueryExecutionCount();

            assertEquals("Entity Graph doit charger les livres sans requêtes supplémentaires",
                    queryCountAvant, queryCountApres);

        } finally {
            em.close();
        }
    }

    @Test
    public void testCacheSecondNiveauEntreDeuxSessionsDistinctes() {
        // Purgation explicite du cache L2 pour garantir un démarrage à froid
        emf.getCache().evictAll();

        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.clear();

        // 1. Première lecture : session 1 (cache L2 froid -> chargement depuis la base et mise en cache L2)
        EntityManager em1 = emf.createEntityManager();
        Auteur a1 = em1.find(Auteur.class, 2L);
        assertNotNull(a1);
        em1.close(); // Fermeture de la session 1 (L1 vidé)

        long hits1 = stats.getSecondLevelCacheHitCount();
        long puts1 = stats.getSecondLevelCachePutCount();

        assertTrue("L'entité doit être injectée dans le Cache L2 lors de la première lecture", puts1 >= 1);

        // 2. Deuxième lecture : session 2 distincte (doit être servie directement depuis le Cache L2)
        EntityManager em2 = emf.createEntityManager();
        Auteur a2 = em2.find(Auteur.class, 2L);
        assertNotNull(a2);
        assertEquals("Fatima Mernissi", a2.getNom());
        em2.close();

        long hits2 = stats.getSecondLevelCacheHitCount();

        assertTrue("La deuxième lecture dans une nouvelle session doit provenir du Cache L2 (Hits accrus)",
                hits2 > hits1);
    }
}
