package fr.dedoublonneur.workflow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dedoublonneur.config.AppProperties;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.CompletedFolderStatus;

/**
 * Liste les dossiers evenement disponibles directement sous le point de montage NAS source.
 */
@Service
public class EventFolderService {

    private static final Logger log = LoggerFactory.getLogger(EventFolderService.class);

    private final AppProperties appProperties;
    private final AnalysisJobRepository analysisJobRepository;

    public EventFolderService(AppProperties appProperties, AnalysisJobRepository analysisJobRepository) {
        this.appProperties = appProperties;
        this.analysisJobRepository = analysisJobRepository;
    }

    public List<EventFolderListing> listAvailableFolders() {
        Map<String, CompletedFolderStatus> completedByFolder = analysisJobRepository.findCompletedFolderStatuses()
                .stream().collect(Collectors.toMap(CompletedFolderStatus::folderName, Function.identity()));
        Path sourceRoot = Path.of(appProperties.nas().sourcePath());
        if (!Files.isDirectory(sourceRoot)) {
            log.warn("Point de montage NAS source introuvable: {}", sourceRoot);
            return List.of();
        }
        try (Stream<Path> entries = Files.list(sourceRoot)) {
                return entries.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .map(folderName -> {
                    CompletedFolderStatus completed = completedByFolder.get(folderName);
                    return new EventFolderListing(folderName, completed != null,
                        completed == null ? null : completed.processedAt(),
                        completed == null ? 0 : Math.max(0, completed.savedBytes()), completed != null);
                    })
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister le dossier NAS source: " + sourceRoot, e);
        }
    }
}
