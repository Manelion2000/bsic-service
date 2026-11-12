package com.bakouan.app.repositories;
import com.bakouan.app.model.BaRequeteOracle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface BaRequeteOracleRepository extends JpaRepository<BaRequeteOracle,String>,JpaSpecificationExecutor<BaRequeteOracle> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from BaRequeteOracle r where r.id=:id") Optional<BaRequeteOracle> locked(@Param("id") String id);
}
