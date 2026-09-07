# Dedoublonneur

Application de tri de photos avant archivage : detection des photos floues et des doublons/quasi-doublons au sein d'un dossier evenement, avec revue manuelle avant copie vers un dossier de sortie (metadonnees EXIF/GPS preservees).

> La documentation fonctionnelle complete (workflow, onglets Flou/Doublons, traitement) sera ajoutee au fur et a mesure de l'implementation des fonctionnalites correspondantes.

## Configuration JVM / Raspberry Pi 4

Le conteneur de production (`docker`/prod profile) cible un Raspberry Pi 4 (4 Go de RAM partagee avec l'OS et les autres conteneurs). Le `Dockerfile` demarre donc l'application avec des options JVM volontairement sobres :

- `-XX:+UseSerialGC` : garbage collector a faible empreinte memoire/CPU, adapte a un environnement mono-cœur/faibles ressources (evite les threads GC concurrents des collecteurs type G1/ZGC).
- `-Xmx384m` : tas Java borne pour laisser de la marge au reste du systeme et des autres conteneurs (PostgreSQL, etc.) sur les 4 Go disponibles.
- `-XX:MaxMetaspaceSize=128m` : borne la metaspace pour eviter toute derive memoire non plafonnee.

Ces valeurs sont un point de depart et pourront etre ajustees lors des tests de charge reels sur le Raspberry Pi (voir `design.md` de la change `add-photo-culling-workflow`, section "Open Questions").
