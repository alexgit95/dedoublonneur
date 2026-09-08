# Dedoublonneur

Application de tri de photos avant archivage : detection des photos floues et des doublons/quasi-doublons au sein d'un dossier evenement, avec revue manuelle avant copie vers un dossier de sortie (metadonnees EXIF/GPS preservees).

## Interface utilisateur

Ouvrez `http://localhost:8686/` (ou l'URL de l'instance distante) pour acceder a l'ecran d'accueil. Cette page unique guide le workflow avec un fil d'etapes : selection du dossier, analyse, revue et export. Les onglets **Flou** et **Doublons** sont regroupes dans l'etape de revue.

L'application reprend automatiquement le workflow courant a partir de `GET /api/workflow/status`. Une analyse en cours affiche sa progression, un dossier pret ouvre la revue, et un traitement termine affiche son recapitulatif. Les anciennes URLs `/flou.html?jobId=...`, `/doublons.html?jobId=...` et `/traiter.html?jobId=...` restent disponibles comme acces directs de compatibilite.

En profil local, SQLite est configure avec un delai d'attente des verrous de 30 secondes et le mode de journalisation par defaut, plus compatible avec les transactions Hibernate utilisees pour generer les identifiants. Le demarrage d'une analyse est egalement serialize dans l'application afin qu'un double clic ou deux onglets ne tentent pas de creer deux workflows simultanement.

## Fonctionnement (workflow)

L'application traite un seul dossier evenement a la fois, en 4 etapes. Pendant l'analyse ou la revue, le bouton **Reinitialiser** demande confirmation, arrete l'analyse si necessaire, supprime les resultats et vignettes du job, puis revient au choix du dossier. Le traitement d'export n'est pas interrompu par ce bouton et le dossier source reste toujours intact. Dans les onglets Flou et Doublons, un maintien d'environ 450 ms sur une vignette ouvre temporairement cette meme vignette en grand ; le relachement la ferme sans telecharger l'original.

L'application traite un seul dossier evenement a la fois, en 4 etapes. Sur l'ecran de selection, un badge **Deja exporte** signale uniquement les dossiers dont un export est termine ; ces dossiers restent selectionnables pour une nouvelle analyse.

L'application traite un seul dossier evenement a la fois, en 4 etapes :

1. **Selection du dossier evenement** — `GET /api/events` liste les sous-dossiers presents sous le point de montage NAS source. `POST /api/events/{folderName}/analysis` demarre l'analyse. Un seul workflow (analyse, revue ou traitement) peut etre actif a la fois ; une tentative concurrente est refusee (HTTP 409). `GET /api/workflow/status` renvoie l'etat courant (`IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, `DONE`) et `POST /api/workflow/cancel` permet de reinitialiser un workflow bloque.
2. **Analyse en tache de fond** — pour chaque photo JPEG ou PNG du dossier (instantane fige au demarrage, les fichiers ajoutes ensuite sont ignores) : calcul d'un score de nettete (variance du Laplacien) et d'un pHash DCT 64 bits, avec une vignette generee et mise en cache. Le pHash detecte les copies et les quasi-doublons de rafale, y compris les photos voisines legerement differentes. La progression est consultable via `GET /api/jobs/{id}` et peut reprendre manuellement (`POST /api/jobs/{id}/resume`) sans recalculer les photos deja traitees.
3. **Revue manuelle** — deux onglets IHM regroupes dans l'ecran racine (`/`) :
   - **Flou** (`GET /api/jobs/{id}/blurred`, pagine) : les photos sous le seuil de nettete sont pre-cochees pour suppression.
   - **Doublons** (`GET /api/jobs/{id}/duplicates?threshold=NN`) : les photos sont regroupees par similarite de hash, recalculee a la demande selon un seuil ajustable (0-100%) sans re-analyser les images. Dans chaque groupe, la photo la plus nette est conservee par defaut (egalite departagee par nom de fichier).
   - Dans les deux onglets, `PATCH /api/photos/{id}` permet de cocher/decocher une photo ; un choix manuel est toujours respecte, meme apres changement du seuil de similarite.
4. **Traitement** (onglet Export de `/`) — `POST /api/jobs/{id}/process` copie (octet a octet, metadonnees EXIF/GPS/dates preservees) toutes les photos conservees et toutes les videos du dossier vers un nouveau dossier de sortie (rejet si le nom existe deja) ; le dossier source n'est jamais modifie. `GET /api/jobs/{id}/result` renvoie le recapitulatif final (photos conservees/supprimees, videos copiees, espace disque avant/apres).

La documentation interactive des endpoints est disponible sur `/swagger-ui.html` une fois l'application demarree.

## Configuration JVM / Raspberry Pi 4

Le conteneur de production (`docker`/prod profile) cible un Raspberry Pi 4 (4 Go de RAM partagee avec l'OS et les autres conteneurs). Le `Dockerfile` demarre donc l'application avec des options JVM volontairement sobres :

- `-XX:+UseSerialGC` : garbage collector a faible empreinte memoire/CPU, adapte a un environnement mono-cœur/faibles ressources (evite les threads GC concurrents des collecteurs type G1/ZGC).
- `-Xmx384m` : tas Java borne pour laisser de la marge au reste du systeme et des autres conteneurs (PostgreSQL, etc.) sur les 4 Go disponibles.
- `-XX:MaxMetaspaceSize=128m` : borne la metaspace pour eviter toute derive memoire non plafonnee.

Ces valeurs sont un point de depart et pourront etre ajustees lors des tests de charge reels sur le Raspberry Pi (voir `design.md` de la change `add-photo-culling-workflow`, section "Open Questions").

## Deploiement sur Arcane (Raspberry Pi)

Ce guide suppose que vous n'avez jamais deploye ce projet et decrit chaque etape depuis Arcane (equivalent de Portainer).

### Prerequis

- Arcane est installe et accessible sur le Raspberry Pi (ou un autre hote Docker).
- Docker fonctionne sur cet hote.
- Le partage NAS contenant vos dossiers evenement est deja monte (ou accessible) depuis l'hote qui execute Docker — Arcane ne monte pas de partage reseau a votre place, il faut que le chemin existe deja cote hote.

### 1. Recuperer l'image Docker

Une GitHub Action construit automatiquement l'image a chaque push et la publie sur Docker Hub :

- Un push sur `main` publie le tag `latest`.
- Un push sur une autre branche publie un tag portant le nom de la branche.
- Un tag Git (release) publie a la fois `latest` et le nom du tag.

Dans le `docker-compose.yml` fourni a la racine du depot, l'image est referencee comme `${DOCKERHUB_USERNAME}/dedoublonneur:latest` — remplacez `DOCKERHUB_USERNAME` par le compte Docker Hub utilise par la GitHub Action (visible dans `.github/workflows/docker-publish.yml`), ou fixez directement le tag voulu.

### 2. Creer la stack dans Arcane

1. Dans Arcane, allez dans **Stacks** → **Add stack**.
2. Donnez un nom (ex: `dedoublonneur`).
3. Collez le contenu du fichier [`docker-compose.yml`](docker-compose.yml) de ce depot.
4. Renseignez les variables d'environnement de la stack (section "Environment variables" d'Arcane) :
   - `APP_DB_PASSWORD` : mot de passe PostgreSQL (obligatoire, la stack refuse de demarrer sans).
   - `NAS_SOURCE_HOST_PATH` : chemin, cote hote, du dossier NAS contenant les dossiers evenement en entree.
   - `NAS_OUTPUT_HOST_PATH` : chemin, cote hote, du dossier NAS ou seront crees les dossiers tries en sortie.
   - `DOCKERHUB_USERNAME` : si vous preferez le passer en variable plutot que de modifier le compose.
5. Cliquez sur **Deploy**.

### 3. Verifications au premier demarrage

- Dans Arcane, les deux conteneurs (`dedoublonneur-app`, `dedoublonneur-postgres`) doivent passer au statut **healthy**.
- Consultez les logs du conteneur `dedoublonneur-app` : vous devez voir Hibernate creer les tables (`create table event ...`, `create table analysis_job ...`, etc.) sans erreur.
- Ouvrez `http://<ip-du-raspberry>:8686/` dans un navigateur : la landing page permet de selectionner un dossier et de reprendre un workflow existant.
- `http://<ip-du-raspberry>:8686/swagger-ui.html` doit afficher la documentation des endpoints.

### 4. Mettre a jour l'application apres un nouveau commit/push

Une fois la nouvelle image publiee par la GitHub Action :

1. Dans Arcane, ouvrez la stack `dedoublonneur`.
2. Utilisez l'action **Pull & redeploy** (ou equivalent) sur le service `app` uniquement.
3. Le service `postgres` et son volume de donnees (`dedoublonneur-postgres-data`) ne sont pas affectes : vos donnees sont conservees.

### 5. Depannage courant

| Symptome | Piste |
|---|---|
| Le dossier evenement n'apparait pas dans `GET /api/events` | Verifiez que `NAS_SOURCE_HOST_PATH` pointe bien vers un chemin existant et lisible cote hote, et que le montage apparait dans le conteneur (`docker exec -it dedoublonneur-app ls /mnt/nas/source`). |
| Erreur de connexion PostgreSQL au demarrage | Verifiez que `APP_DB_PASSWORD` est identique pour les deux services (c'est le cas par defaut dans le compose fourni, car la meme variable est reutilisee), et que le conteneur `postgres` est bien `healthy` avant que `app` ne demarre (la dependance `depends_on: condition: service_healthy` s'en charge normalement). |
| Le conteneur `app` redemarre en boucle | Consultez ses logs dans Arcane : une erreur au demarrage (schema Hibernate, connexion DB) y est generalement visible dans les premieres secondes. |
