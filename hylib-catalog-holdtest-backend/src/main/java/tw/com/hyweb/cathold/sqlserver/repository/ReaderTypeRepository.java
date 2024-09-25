package tw.com.hyweb.cathold.sqlserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.util.Streamable;

import tw.com.hyweb.cathold.sqlserver.model.ReaderType;

public interface ReaderTypeRepository extends ReadOnlyRepository<ReaderType, Integer> {

	List<ReaderType> findByReaderTypeId(int readerTypeId);

	Streamable<ReaderType> findByReaderTypeCodeIn(List<String> lastitemReadertype);

	Optional<ReaderType> findByReaderTypeCode(String typeCode);

}
