package tw.com.hyweb.cathold.sqlserver.repository;

import java.util.Optional;

import tw.com.hyweb.cathold.sqlserver.model.MarcDetail;

public interface MarcDetailRepository extends ReadOnlyRepository<MarcDetail, Integer> {

	Optional<MarcDetail> findByMarcId(int marcId);

}
