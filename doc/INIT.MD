Application preparation de blibliotheque phott

Objectifs : avoir une application mobile qui me permettre de faore le tri dans mes photos avant de les ranger en supprimant les doublon (et gardant la meilleur photo) et en supprimant toutes les photos trop flou

Comment ca doit marcher : 
L'application va etre installé sur une machine avec un point de montage
Le point de montage pointra sur un NAS avec un repertoire de travail , ou sont mise toutes les photos dans des sous repertoires portant le nom de l'evenement
Ce que j'aimerais c'est une application de type workflow simple
Sur la page web je choisi un des sous repertoires evenement à analyser, ensuite  en tache de fond l'application trie les photos, regroupe les doubles et identifie les trop flou.
Quand l'analyse est terminé sur mon IHM je retrouve le repertoire en cours de travail, j'ai un onglet avec tous les top flou (cohcés par fefaut pour etre supprimer et que je peux decocher si je veux garder)
Un autre onglet doublon ou je retrouve toutes les photos en double triple ou plus regroupé par similartité et cocher pour etre supprimer, en gardant decoché la meilleur photo

une fois que j'ai fait mes choix de cocher decocher, un bouton traiter le dossier est cliqué, on me demande de renseigné un nom de dossier de sortie, et toutes les photos conservé devront etre copié dans ce repertoire de sortie, ainsi que mes videos presente dans le repertoire source.
au moment du clic sur traité je dois avoir un recapitulatif, nombre de photo conservé / supprimé espace disque final vs espace gagné.

Attention je ne peux traité qu'un seul dossier à la fois , je ne peux pas commencer l'analyse d'un nouveau dossier si le precedent n'est pas terminé.

ATTENTION toutes les metadata du fichier image doivent etre conservé ( date de creation / modification, coordonnées GPS EXIF data etc...).


L'application doit tourner sur docker , sur un rapsberry pi.
