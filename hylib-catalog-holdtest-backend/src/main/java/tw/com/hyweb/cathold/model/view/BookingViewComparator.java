package tw.com.hyweb.cathold.model.view;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import tw.com.hyweb.cathold.model.Booking;
import tw.com.hyweb.cathold.model.Phase;

public class BookingViewComparator implements Comparator<Booking>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3804344470378373642L;

	private static final List<Phase> SORT_PHASES = List.of(Phase.AVAILABLE, Phase.A01_ORDER, Phase.CAB_WAIT,
			Phase.WAIT_ANNEX, Phase.TRANSIT_B, Phase.DISTRIBUTION, Phase.PLACE, Phase.SUSPENSION,
			Phase.OVERDUE_BOOKING_WAITING);

	public static final Comparator<Booking> INSTANCE = new BookingViewComparator();

	private BookingViewComparator() {
	}

	@Override
	public int compare(Booking b1, Booking b2) {
		Phase phase1 = b1.getPhase();
		Phase phase2 = b2.getPhase();
		if (SORT_PHASES.contains(phase1)) {
			if (!SORT_PHASES.contains(phase2))
				return -1;
			if (phase1 != phase2)
				return SORT_PHASES.indexOf(phase1) < SORT_PHASES.indexOf(phase2) ? -1 : 1;
			return this.comparingNull(Booking::getDueDate).thenComparing(this.comparingNull(Booking::getAvailableDate))
					.thenComparing(this.comparingNull(Booking::getTransitDate))
					.thenComparing(this.comparingNull(Booking::getDistributeDate))
					.thenComparing(Comparator.comparing(Booking::getPlaceDate))
					.thenComparing(Comparator.comparingLong(Booking::getId)).compare(b1, b2);
		}
		return SORT_PHASES.contains(phase2) ? 1 : Comparator.comparingLong(Booking::getId).compare(b1, b2);
	}

	private <T, U extends Comparable<? super U>> Comparator<T> comparingNull(Function<T, ? extends U> keyExtractor) {
		return Comparator.comparing(keyExtractor, Comparator.nullsLast(Comparator.naturalOrder()));
	}

}
