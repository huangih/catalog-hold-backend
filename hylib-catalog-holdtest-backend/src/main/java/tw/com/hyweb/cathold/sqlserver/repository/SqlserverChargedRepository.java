package tw.com.hyweb.cathold.sqlserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;

import tw.com.hyweb.cathold.sqlserver.model.SqlserverCharged;

public interface SqlserverChargedRepository extends ReadOnlyRepository<SqlserverCharged, Long> {

	public List<SqlserverCharged> findByHoldIdIn(List<Integer> holdIds, Sort sort);

	public List<SqlserverCharged> findByHoldIdIn(Integer[] holdIds);

	public Optional<SqlserverCharged> findByHoldId(int holdId);

	public Optional<SqlserverCharged> findByHoldIdAndReaderId(int holdId, int readerId);

	public Streamable<SqlserverCharged> findByReaderId(int readerId);
}
