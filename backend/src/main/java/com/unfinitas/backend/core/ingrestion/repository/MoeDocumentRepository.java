package com.unfinitas.backend.core.ingrestion.repository;

import com.unfinitas.backend.core.ingrestion.model.MoeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MoeDocumentRepository extends JpaRepository<MoeDocument, UUID> {

}
