
package com.manan.cloudfilemanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.manan.cloudfilemanager.entity.FileMetaData;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetaData, Long> {

    List<FileMetaData> findByUser_Id(Long userId);

    Page<FileMetaData> findByUser_Id(Long userId, Pageable pageable);

    Optional<FileMetaData> findByIdAndUser_Id(Long id, Long userId);

    Page<FileMetaData> findByUser_IdAndOriginalFileNameContainingIgnoreCase(
            Long userId,
            String fileName,
            Pageable pageable
    );

    boolean existsByIdAndUser_Id(Long id, Long userId);
}
