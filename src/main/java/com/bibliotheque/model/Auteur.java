package com.bibliotheque.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant un Auteur dans la bibliothèque numérique.
 * Activée pour le Cache de Second Niveau (L2 Cache) via @Cacheable et @Cache.
 * Déclare un EntityGraph nommé 'Auteur.livres' pour résoudre le problème N+1.
 *
 * @author Houda El Amrani
 */
@Entity
@Table(name = "auteurs")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraph(
    name = "Auteur.livres",
    attributeNodes = @NamedAttributeNode("livres")
)
public class Auteur implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(length = 80)
    private String nationalite;

    @Column(length = 255)
    private String biographie;

    /**
     * Association 1-N vers Livre en mode LAZY pour reproduire et résoudre le problème N+1.
     * Pour mettre en cache la collection dans le L2 cache, il est INDISPENSABLE
     * de poser l'annotation @Cache également sur la collection elle-même.
     */
    @OneToMany(mappedBy = "auteur", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Livre> livres = new ArrayList<>();

    public Auteur() {
    }

    public Auteur(String nom, String nationalite, String biographie) {
        this.nom = nom;
        this.nationalite = nationalite;
        this.biographie = biographie;
    }

    public Auteur(String nom) {
        this(nom, "Marocaine", "Auteur contemporain");
    }

    // Méthodes d'assistance pour synchroniser les deux côtés de la relation
    public void ajouterLivre(Livre livre) {
        livres.add(livre);
        livre.setAuteur(this);
    }

    public void retirerLivre(Livre livre) {
        livres.remove(livre);
        livre.setAuteur(null);
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNationalite() {
        return nationalite;
    }

    public void setNationalite(String nationalite) {
        this.nationalite = nationalite;
    }

    public String getBiographie() {
        return biographie;
    }

    public void setBiographie(String biographie) {
        this.biographie = biographie;
    }

    public List<Livre> getLivres() {
        return livres;
    }

    public void setLivres(List<Livre> livres) {
        this.livres = livres;
    }

    @Override
    public String toString() {
        return "Auteur[ID=" + id + " | Nom='" + nom + "' | Nationalité='" + nationalite + "']";
    }
}
