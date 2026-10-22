package com.bibliotheque.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Entité représentant un Livre publié.
 * Mise en cache L2 via @Cacheable et @Cache(usage = READ_WRITE).
 *
 * @author Houda El Amrani
 */
@Entity
@Table(name = "livres")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Livre implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String titre;

    @Column(length = 30)
    private String isbn;

    @Column(name = "annee_publication")
    private int anneePublication;

    @Column(name = "prix_dh")
    private double prix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private Auteur auteur;

    public Livre() {
    }

    public Livre(String titre, String isbn, int anneePublication, double prix) {
        this.titre = titre;
        this.isbn = isbn;
        this.anneePublication = anneePublication;
        this.prix = prix;
    }

    public Livre(String titre) {
        this(titre, "978-2-0000-0000", 2024, 75.0);
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getAnneePublication() {
        return anneePublication;
    }

    public void setAnneePublication(int anneePublication) {
        this.anneePublication = anneePublication;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public Auteur getAuteur() {
        return auteur;
    }

    public void setAuteur(Auteur auteur) {
        this.auteur = auteur;
    }

    @Override
    public String toString() {
        return "Livre[ID=" + id + " | Titre='" + titre + "' | Année=" + anneePublication + " | Prix=" + prix + " DH]";
    }
}
