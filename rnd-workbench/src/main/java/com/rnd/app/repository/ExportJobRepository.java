package com.rnd.app.repository; import com.rnd.app.entity.ExportJob; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface ExportJobRepository extends JpaRepository<ExportJob,Long>{ Optional<ExportJob> findByIdAndCreatorId(Long id,Long creatorId); }
