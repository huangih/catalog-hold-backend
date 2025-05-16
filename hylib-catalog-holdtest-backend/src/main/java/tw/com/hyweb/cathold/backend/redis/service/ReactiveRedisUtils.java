package tw.com.hyweb.cathold.backend.redis.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.redisson.api.RBucketReactive;
import org.redisson.api.RExpirableReactive;
import org.redisson.api.RKeysReactive;
import org.redisson.api.RListReactive;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@Component
@RequiredArgsConstructor
public class ReactiveRedisUtils {

	private static final String RWLOCK = "_rwLock";

	private final RedissonReactiveClient client;

	private Consumer<Tuple3<String, List<?>, Duration>> listCacheConsumer;

	private Consumer<Tuple3<String, Object, Duration>> monoCacheConsumer;

	@PostConstruct
	void init() {
		Flux.create(sink -> this.listCacheConsumer = sink::next).cast(Tuple3.class).delayElements(Duration.ofMillis(10))
				.subscribe(tup3 -> this.redisListCacheProcess(String.class.cast(tup3.getT1()), (List<?>) tup3.getT2(),
						Duration.class.cast(tup3.getT3())));
		Flux.create(sink -> this.monoCacheConsumer = sink::next).cast(Tuple3.class).delayElements(Duration.ofMillis(10))
				.subscribe(tup3 -> this.redisLockCacheProcess(String.class.cast(tup3.getT1()), tup3.getT2(),
						Duration.class.cast(tup3.getT3())));
	}

	public Mono<Boolean> hasKey(String key) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).readLock();
		return rLock.tryLock(10, 5, TimeUnit.SECONDS).filter(b -> b).flatMap(b -> this.client.getKeys().countExists(key)
				.map(n -> n > 0).doFinally(s -> rLock.forceUnlock().subscribe()));
	}

	public <T> Mono<T> getBuketFromRedis(String key, Boolean reExpire, Duration duration) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).readLock();
		RBucketReactive<T> rObj = this.client.getBucket(key);
		return rLock.tryLock(2000, 1500, TimeUnit.MILLISECONDS).filter(b -> b)
				.flatMap(b -> rObj.isExists().filter(b1 -> b1)
						.flatMap(b1 -> this.getMonoReExipe(rObj, reExpire, duration))
						.doFinally(s -> rLock.forceUnlock().subscribe()));
	}

	public <T> Mono<T> getMonoFromRedis(String key, Duration duration) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).readLock();
		RBucketReactive<T> rObj = this.client.getBucket(key);
		return rLock.tryLock(30, 5, TimeUnit.SECONDS).filter(b -> b)
				.flatMap(b -> rObj.isExists().filter(b1 -> b1).flatMap(b1 -> this.getMonoReExipe(rObj, null, duration))
						.doFinally(s -> rLock.forceUnlock().subscribe()));
	}

	private <T> Mono<T> getMonoReExipe(RBucketReactive<T> buket, Boolean reExpire, Duration duration) {
		if (duration == null)
			duration = Duration.ofDays(1);
		if (Boolean.FALSE.equals(reExpire))
			return buket.get();
		return buket.getAndExpire(duration);
	}

	public <T> Mono<T> redisMonoCache(String key, Mono<T> objectMono, Duration duration) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).writeLock();
		RBucketReactive<T> rObj = this.client.getBucket(key);
		if (duration == null)
			duration = Duration.ofDays(1);
		final Duration fDuration = duration;
		return rLock.tryLock(2000, 1500, TimeUnit.MILLISECONDS).filter(b -> b).flatMap(b -> objectMono.map(obj -> {
			rObj.set(obj, fDuration);
			return obj;
		}).doFinally(s -> rLock.forceUnlock().subscribe()));
	}

	public void redisLockCache(String key, Object object, Object duration) {
		Tuple3<String, Object, Duration> tup3 = Tuples.of(key, object, this.getAssingTimeDuration(duration));
		this.monoCacheConsumer.accept(tup3);
	}

	private Duration getAssingTimeDuration(Object object) {
		if (object == null)
			return Duration.ofDays(1);
		if (object instanceof LocalDate)
			return Duration.ofMinutes(
					ChronoUnit.MINUTES.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()) + 2);
		if (object instanceof LocalDateTime dateTime)
			return Duration.ofMinutes(ChronoUnit.MINUTES.between(LocalDateTime.now(), dateTime));
		return Duration.class.cast(object);
	}

	private <T> void redisLockCacheProcess(String key, T object, Duration duration) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).writeLock();
		RBucketReactive<T> rObj = this.client.getBucket(key);
		rLock.tryLock(2000, 1500, TimeUnit.MILLISECONDS).filter(b -> b).flatMap(b -> rObj.set(object, duration))
				.doFinally(s -> rLock.forceUnlock().subscribe()).subscribe();
	}

	public <T> Mono<T> saveForCache(String key, T obj, Duration duration) {
		if (duration == null)
			duration = Duration.ofDays(1);
		RBucketReactive<T> buket = this.client.getBucket(key);
		return buket.set(obj, duration).thenReturn(obj);
	}

	public <T> Mono<List<T>> getMonoListFromRedis(String key, Class<T> clazz, boolean reExpire, Duration duration) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).readLock();
		RBucketReactive<List<T>> rObj = this.client.getBucket(key);
		return rLock.tryLock(2000, 1500, TimeUnit.MILLISECONDS).filter(b -> b)
				.flatMap(b -> rObj.isExists().filter(b1 -> b1)
						.flatMap(b1 -> this.getMonoListReexpire(rObj, clazz, reExpire, duration))
						.doFinally(s -> rLock.forceUnlock().subscribe()));
	}

	private <T> Mono<List<T>> getMonoListReexpire(RBucketReactive<List<T>> buket, Class<T> clazz, boolean reExpire,
			Duration duration) {
		if (!reExpire)
			return buket.get().flatMapMany(Flux::fromIterable).cast(clazz).collectList();
		if (duration == null)
			duration = Duration.ofDays(1);
		return buket.getAndExpire(duration).flatMapMany(Flux::fromIterable).cast(clazz).collectList();
	}

	public <T> Flux<T> getFluxFromRedis(String key, boolean reExpire) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).readLock();
		RListReactive<T> rObj = this.client.getList(key);
		return rLock.tryLock(2000, 6000, TimeUnit.MILLISECONDS).filter(b -> b).flatMapMany(
				b -> rObj.isExists().filter(b1 -> b1).flatMapMany(b1 -> this.getFluxByIdKey(rObj, reExpire))
						.doFinally(s -> rLock.forceUnlock().subscribe()));
	}

	public void redisListCache(String key, List<?> list, Object duration) {
		Tuple3<String, List<?>, Duration> tup3 = Tuples.of(key, list, this.getAssingTimeDuration(duration));
		this.listCacheConsumer.accept(tup3);
	}

	private <T> void redisListCacheProcess(String key, List<T> list, Duration duration) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).writeLock();
		RListReactive<T> rList = this.client.getList(key);
		rLock.tryLock(2000, 1500, TimeUnit.MILLISECONDS).filter(b -> b)
				.flatMap(b -> rList.delete().flatMap(n -> rList.addAll(list).flatMap(b1 -> rList.expire(duration)))
						.doFinally(s -> rLock.forceUnlock().subscribe()))
				.subscribe();
	}

	private <T> Flux<T> getFluxByIdKey(RListReactive<T> rList, boolean reExpire) {
		return rList.iterator().doOnComplete(() -> {
			if (reExpire)
				rList.expire(Duration.ofDays(1)).subscribe();
			else
				this.expireAt(rList, null);
		});
	}

	public <R> Mono<R> getMonoFromLock(String key, Supplier<Mono<R>> supplier) {
		RLockReactive rLock = this.client.getLock(key);
		return rLock.tryLock(5000, TimeUnit.MILLISECONDS).filter(b -> b)
				.flatMap(b -> supplier.get().doFinally(obj -> rLock.forceUnlock().subscribe()));
	}

	public <R> Mono<R> getMonoFromReadLock(String key, Supplier<Mono<R>> supplier) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).readLock();
		return rLock.tryLock(2000, TimeUnit.MILLISECONDS).filter(b -> b)
				.flatMap(b -> supplier.get().doFinally(obj -> rLock.forceUnlock().subscribe()))
				.switchIfEmpty(Mono.empty());
	}

	public <R> Mono<R> getMonoFromWriteLock(String key, Supplier<Mono<R>> supplier) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).writeLock();
		return rLock.tryLock(3000, 5000, TimeUnit.MILLISECONDS).filter(b -> b)
				.flatMap(b -> supplier.get().doFinally(obj -> rLock.forceUnlock().subscribe()))
				.switchIfEmpty(Mono.empty());
	}

	private void expireAt(RExpirableReactive key, Instant instant) {
		final Instant inst = instant != null ? instant
				: LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
		key.expire(inst).subscribe();
	}

	public void expireAt(String key, Timestamp timestamp) {
		if (timestamp == null)
			timestamp = Timestamp.valueOf(LocalDate.now().atTime(23, 59, 59));
		long expireTime = timestamp.getTime();
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).writeLock();
		rLock.tryLock(2000, 1500, TimeUnit.MILLISECONDS).filter(b -> b).flatMap(
				b -> this.client.getKeys().expireAt(key, expireTime).doFinally(s -> rLock.forceUnlock().subscribe()))
				.subscribe();
	}

	public void unlink(String key) {
		RLockReactive rLock = this.client.getReadWriteLock(key + RWLOCK).writeLock();
		rLock.tryLock(2000, 1500, TimeUnit.MILLISECONDS).handle((b, sink) -> {
			if (Boolean.TRUE.equals(b))
				this.client.getKeys().unlink(key).doFinally(s -> rLock.unlock().subscribe());
			sink.complete();
		}).subscribe();
	}

	public Mono<Void> unlinkKeys(String pattern) {
		RKeysReactive rKeys = this.client.getKeys();
		return rKeys.getKeys(KeysScanOptions.defaults().pattern(pattern)).flatMap(rKeys::unlink, 8).then();
	}

}
