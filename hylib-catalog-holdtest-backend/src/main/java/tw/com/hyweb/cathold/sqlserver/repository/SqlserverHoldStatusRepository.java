package tw.com.hyweb.cathold.sqlserver.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.hyweb.cathold.sqlserver.model.SqlserverHoldStatus;

public interface SqlserverHoldStatusRepository extends JpaRepository<SqlserverHoldStatus, Integer> {

	Optional<SqlserverHoldStatus> findByHoldId(int holdId);

}
