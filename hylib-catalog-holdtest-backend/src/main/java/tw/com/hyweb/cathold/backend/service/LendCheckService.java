package tw.com.hyweb.cathold.backend.service;

import reactor.core.publisher.Mono;
import tw.com.hyweb.cathold.model.LendCallback;
import tw.com.hyweb.cathold.model.LendCheck;

public interface LendCheckService {

	Mono<LendCallback> prepareLendCheck(LendCallback lendCallback);

	Mono<LendCheck> checkRfidUidMap(LendCallback lendCallback); //查檢RFID UID對照表

	Mono<LendCheck> lastItemMissing(LendCallback lendCallback); //是否為有預約者的最後一件去向不明

	Mono<LendCheck> onAvailBooking(LendCallback lendCallback); //是否為預約到館借閱

	Mono<LendCheck> onTransit(LendCallback lendCallback);// 書是否處於調撥

	Mono<LendCheck> onBookingAvailRemove(LendCallback lendCallback); //是否為待預約撤架

	Mono<LendCheck> onUserBooking(LendCallback lendCallback);// 借閱者是否已預約此書

	Mono<LendCheck> onBookingDistribution(LendCallback lendCallback); // 借閱之資料正分配架上找書中(排除借予missing)

	Mono<LendCheck> onCMissingLend(LendCallback lendCallback); //容許且僅可借閱"調撥異常"資料借閱者類型之借閱
	
	Mono<LendCheck> onUserLendCallVolIds(LendCallback lendCallback); //是否為借閱中同一callvolId的書
	
	Mono<LendCheck> putLendCallback(LendCallback lendCallback);

	void lendCallback(String callbackId);

}
