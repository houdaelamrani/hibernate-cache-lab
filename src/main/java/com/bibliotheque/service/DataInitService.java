package com.bibliotheque.service;

import com.bibliotheque.model.Auteur;
import com.bibliotheque.model.Livre;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * Service d'initialisation du jeu de données d'évaluation.
 * Crée 10 auteurs renommés avec chacun 5 ouvrages emblématiques (50 livres au total).
 *
 * @author Houda El Amrani
 */
public class DataInitService {

    public void initData(EntityManager em) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            String[][] auteursData = {
                {"Tahar Ben Jelloun", "Marocaine", "Prix Goncourt 1987"},
                {"Fatima Mernissi", "Marocaine", "Sociologue et écrivaine féministe"},
                {"Driss Chraïbi", "Marocaine", "Pionnier de la littérature moderne"},
                {"Leïla Slimani", "Marocaine/Française", "Prix Goncourt 2016"},
                {"Abdellah Taïa", "Marocaine", "Écrivain et cinéaste"},
                {"Albert Camus", "Française", "Prix Nobel de Littérature 1957"},
                {"Victor Hugo", "Française", "Poète et dramaturge majeur"},
                {"Gabriel García Márquez", "Colombienne", "Prix Nobel de Littérature 1982"},
                {"Haruki Murakami", "Japonaise", "Auteur contemporain à succès mondial"},
                {"Chimamanda Ngozi Adichie", "Nigériane", "Écrivaine et essayiste"}
            };

            for (int i = 0; i < auteursData.length; i++) {
                String nom = auteursData[i][0];
                String nationalite = auteursData[i][1];
                String bio = auteursData[i][2];

                Auteur auteur = new Auteur(nom, nationalite, bio);

                for (int j = 1; j <= 5; j++) {
                    String titre = "Ouvrage " + j + " - " + nom;
                    String isbn = String.format("978-2-%04d-%04d", (i + 1) * 11, j * 77);
                    int annee = 1980 + (i * 3) + j;
                    double prix = 65.0 + (j * 12.5);

                    Livre livre = new Livre(titre, isbn, annee, prix);
                    auteur.ajouterLivre(livre);
                }

                em.persist(auteur);
            }

            tx.commit();
            System.out.println("✅ Initialisation réussie : 10 auteurs et 50 livres insérés en base.");
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Erreur lors de l'initialisation des données", e);
        }
    }
}
