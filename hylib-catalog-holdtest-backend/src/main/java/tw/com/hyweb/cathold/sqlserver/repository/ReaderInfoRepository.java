package tw.com.hyweb.cathold.sqlserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.util.Streamable;

import tw.com.hyweb.cathold.sqlserver.model.ReaderInfo;

public interface ReaderInfoRepository extends ReadOnlyRepository<ReaderInfo, Integer> {

	Optional<ReaderInfo> findByReaderId(int readerId);

	Optional<ReaderInfo> findByReaderIdAndReaderTypeId(int readerId, int readerType);

	Streamable<ReaderInfo> findByReaderTypeIdIn(List<Integer> readerTypes);

}
