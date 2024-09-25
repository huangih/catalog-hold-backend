package tw.com.hyweb.cathold.sqlserver.repository;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface ReadOnlyRepository<T, I> extends Repository<T, I> {

	Optional<T> findById(I id);

	Stream<T> findAll();
}
