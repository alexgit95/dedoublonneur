package fr.dedoublonneur.workflow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dedoublonneur.config.AppProperties;

/**
 * Liste les dossiers evenement disponibles directement sous le point de montage NAS source.
 */
@Service
public class EventFolderService {

    private static final Logger log = LoggerFactory.getLogger(EventFolderService.class);

    private final AppProperties appProperties;

    public EventFolderService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public List<String> listAvailableFolders() {
        Path sourceRoot = Path.of(appProperties.nas().sourcePath());
        if (!Files.isDirectory(sourceRoot)) {
            log.warn("Point de montage NAS source introuvable: {}", sourceRoot);
            return List.of();
        }
        try (Stream<Path> entries = Files.list(sourceRoot)) {
            return entries.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister le dossier NAS source: " + sourceRoot, e);
        }
    }
}
