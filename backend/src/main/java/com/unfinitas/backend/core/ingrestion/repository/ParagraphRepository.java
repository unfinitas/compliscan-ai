package com.unfinitas.backend.core.ingrestion.repository;

import com.unfinitas.backend.core.ingrestion.model.MoeDocument;
import com.unfinitas.backend.core.ingrestion.model.Paragraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParagraphRepository extends JpaRepository<Paragraph, Long> {
   List<Paragraph> findByMoeDocument(MoeDocument moeDocument);
}
