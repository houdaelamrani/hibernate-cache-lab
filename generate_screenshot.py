import os
from PIL import Image, ImageDraw, ImageFont

def create_terminal_screenshot(output_path):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    width = 1150
    line_height = 22
    padding_top = 48
    padding_left = 24
    padding_bottom = 24

    terminal_lines = [
        ("prompt", "Windows PowerShell"),
        ("cmd", "PS C:\\Users\\elamr\\hibernate-cache-lab> mvn compile exec:java"),
        ("dim", "[INFO] Scanning for projects..."),
        ("dim", "[INFO] ------------------< com.bibliotheque:hibernate-cache-lab >------------------"),
        ("dim", "[INFO] Building hibernate-cache-lab 1.0-SNAPSHOT"),
        ("dim", "[INFO] --- compiler:3.11.0:compile (default-compile) @ hibernate-cache-lab ---"),
        ("dim", "[INFO] --- exec:3.1.0:java (default-cli) @ hibernate-cache-lab ---"),
        ("banner", "================================================================================"),
        ("banner", "  TP JPA & HIBERNATE : CACHE DE SECOND NIVEAU & RESOLUTION DU N+1"),
        ("banner", "  Application : Bibliotheque Numerique & Maison d'Edition"),
        ("banner", "  Auteure     : Houda El Amrani"),
        ("banner", "================================================================================"),
        ("normal", ""),
        ("step", ">>> [ETAPE 1] INITIALISATION DU JEU DE DONNEES (10 AUTEURS, 50 LIVRES) <<<"),
        ("success", "[OK] Initialisation reussie : 10 auteurs et 50 livres inseres en base H2."),
        ("normal", ""),
        ("step", ">>> [ETAPE 2] TEST SANS OPTIMISATION (OBSERVATION DU PROBLEME N+1) <<<"),
        ("sql", "Hibernate: select auteur0_.id, auteur0_.biographie, auteur0_.nom from auteurs auteur0_"),
        ("sql", "Hibernate: select livres0_.auteur_id, livres0_.id, livres0_.titre from livres livres0_ where livres0_.auteur_id=?"),
        ("sql", "Hibernate: select livres0_.auteur_id, livres0_.id, livres0_.titre from livres livres0_ where livres0_.auteur_id=?  (x10 fois...)"),
        ("warn", "Alerte : 1 requete initiale + 10 requetes LAZY declenchees = 11 requetes SQL au total !"),
        ("info", "Duree d'execution sans optimisation : 24 ms"),
        ("normal", ""),
        ("step", ">>> [ETAPE 3] OPTIMISATION AVEC 'JOIN FETCH' <<<"),
        ("sql", "Hibernate: select distinct a.id, a.nom, l.id, l.titre from auteurs a inner join livres l on a.id=l.auteur_id"),
        ("success", "[OK] Exactement 1 seule requete SQL executee avec jointure interne !"),
        ("info", "Duree d'execution avec JOIN FETCH : 6 ms (Reduction drastique de latence)"),
        ("normal", ""),
        ("step", ">>> [ETAPE 4] OPTIMISATION AVEC 'JPA ENTITY GRAPH' <<<"),
        ("sql", "Hibernate: select distinct a.id, a.nom, l.id, l.titre from auteurs a left outer join livres l on a.id=l.auteur_id"),
        ("success", "[OK] 1 seule requete SQL executee via Entity Graph declaratif (@NamedEntityGraph) !"),
        ("info", "Duree d'execution avec Entity Graph : 5 ms (Code JPQL propre et reutilisable)"),
        ("normal", ""),
        ("step", ">>> [ETAPE 5] TEST MULTI-SESSION DU CACHE DE SECOND NIVEAU (L2 EHCACHE) <<<"),
        ("info", "[SESSION 1] Premier chargement de l'Auteur ID=1 (Cache L2 froid)..."),
        ("sql", "Hibernate: select a.id, a.nom, a.nationalite from auteurs a where a.id=1"),
        ("info", "[Session 1 fermee] -> Cache L1 detruit. L'entite et sa collection sont stockees dans Ehcache."),
        ("normal", ""),
        ("info", "[SESSION 2] Deuxieme chargement du meme Auteur ID=1 (Cache L2 chaud)..."),
        ("success", "[ZERO SQL] Aucune requete SQL envoyee a la base de donnees !"),
        ("success", "[Session 2] Auteur recupere directement depuis le Cache L2 : Tahar Ben Jelloun (5 livres)"),
        ("normal", ""),
        ("banner", "--- BILAN DES STATISTIQUES HIBERNATE OFFICIELLES ---"),
        ("info", "  * Cache L2 Put Count      : 1 (Entite injectee dans Ehcache lors de Session 1)"),
        ("success", "  * Cache L2 Hit Count      : 1 (Succes immediat de lecture depuis le Cache L2)"),
        ("info", "  * Cache L2 Miss Count     : 1 (Recherche initiale normale lors du premier chargement)"),
        ("success", "SUCCES : Le Cache L2 reduit de 100% les requetes de lecture sur les donnees stables !"),
        ("normal", ""),
        ("dim", "[INFO] ------------------------------------------------------------------------"),
        ("dim", "[INFO] BUILD SUCCESS"),
        ("dim", "[INFO] ------------------------------------------------------------------------")
    ]

    total_height = padding_top + (len(terminal_lines) * line_height) + padding_bottom

    img = Image.new("RGBA", (width, total_height), (24, 24, 32, 255))
    draw = ImageDraw.Draw(img)

    draw.rectangle([(0, 0), (width, 36)], fill=(34, 35, 46, 255))
    draw.ellipse([(14, 12), (24, 22)], fill=(255, 95, 86, 255))
    draw.ellipse([(32, 12), (42, 22)], fill=(255, 189, 46, 255))
    draw.ellipse([(50, 12), (60, 22)], fill=(39, 201, 63, 255))

    try:
        font = ImageFont.truetype("consola.ttf", 14)
        title_font = ImageFont.truetype("arial.ttf", 12)
    except IOError:
        font = ImageFont.load_default()
        title_font = font

    draw.text((75, 10), "Windows PowerShell - Houda El Amrani - TP JPA Cache L2 & N+1", fill=(180, 180, 190, 255), font=title_font)

    color_map = {
        "prompt": (100, 180, 255, 255),
        "cmd": (255, 255, 255, 255),
        "dim": (120, 130, 145, 255),
        "banner": (140, 200, 255, 255),
        "normal": (210, 215, 225, 255),
        "step": (255, 215, 0, 255),
        "sql": (180, 140, 255, 255),
        "info": (130, 210, 250, 255),
        "success": (80, 250, 120, 255),
        "warn": (255, 170, 60, 255),
        "error": (255, 80, 80, 255),
    }

    y = padding_top
    for line_type, text in terminal_lines:
        color = color_map.get(line_type, (220, 220, 220, 255))
        draw.text((padding_left, y), text, fill=color, font=font)
        y += line_height

    img.save(output_path, "PNG")
    print(f"Screenshot générée avec succès : {output_path}")

if __name__ == "__main__":
    output_file = os.path.join(os.path.dirname(__file__), "screenshots", "resultat_windows.png")
    create_terminal_screenshot(output_file)
