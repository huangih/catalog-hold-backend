package tw.com.hyweb.cathold.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public enum ResultPhase {
	SUCCESS("OK"), NOSUCH_BOOKING("NB"), NOSAME_READER("NR"), NOT_ONAVAIL("OA"), LIMIT_EXPNUM("EN"),
	LIMIT_BOOKINGNUM("EB"), HAD_EXPEND("HE"), CHG_PICKSITE_ONTRANS("CT"), CHG_PICKSITE_ONAVAIL("CA"),
	DENY_PICKSITE("CP"), SYSTEM_EXCEPTION("SE"), HAD_BOOKING("HB"), DATE_END_BEFORE_BEGIN("DB"), CANNOT_ROLLBACK("CR"),
	DATE_END_BEFORE_TODAY("DT"), USER_ON_STOP_BOOKING("US"), ZERO_ALLOWBOOKINGNUM("BZ"), NONE_SUSPENSION("NS"),
	HOLD_ON_TRANSIT_B("TB"), NOT_BOOKING_USER("NU"), LASITEM_ALLOWBOOKING("LA"), SAMECALLVOLID_ONLEND("SC"),
	NOTON_UIDMAP("NM"), NOT_MOBILELEND_SITE("SN"), CANNOT_EXPAND_PICKUPSITE("NE"), HOTBOOK_NOT_EXPAND("EH"),
	CANNOT_CANCEL_BOOKINGPHASE("NP"), NOMATCH_ACTION("NA"), HOLD_NOTEXISTS("HE"), NOSAME_MARCID("SM"),
	NOMATCH_CMISSING("CL"), NOTALLOW_HOTTYPE("Ph"), NOTALLOW_AVTYPE("Pa"), NOTALLOW_PERIODTYPE("Pp"),
	NOTALLOW_ANNEX("Pe"), NOTALLOW_BOOKINGNUM("Pb"), NOTALLOW_USERTYPE("Pu"), NOTON_STOPBOOKING("oS"),
	TRADEOFF_HASBOOKING("Tb"), NO_TRADEOFF("nT"), ORISTOPBOOKING_HADEND("OE");

	@Getter
	private String name;

}
