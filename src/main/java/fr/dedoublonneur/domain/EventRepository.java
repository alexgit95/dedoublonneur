package fr.dedoublonneur.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Transactional(readOnly = true)
    Optional<Event> findByFolderName(String folderName);
}
