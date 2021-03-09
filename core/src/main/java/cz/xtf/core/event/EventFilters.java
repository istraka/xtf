package cz.xtf.core.event;

import java.time.ZonedDateTime;
import java.util.function.Predicate;

import cz.xtf.core.event.helpers.EventHelper;
import io.fabric8.kubernetes.api.model.Event;

public class EventFilters {

    /**
     * Filter events of types defined in the array (case insensitive).
     * For example: {@code Warning}, {@code Normal}, ...
     */
    public static Predicate<Event> ofEventTypes(String... types) {
        return event -> isStrInArrayCaseInsensitive(event.getType(), types);
    }

    /**
     * Filter events with involved object kind defined in the array (case insensitive).
     * For example: {@code Warning}, {@code Normal}, ...
     */
    public static Predicate<Event> ofObjKinds(String... kinds) {
        return event -> isStrInArrayCaseInsensitive(event.getInvolvedObject().getKind(), kinds);
    }

    /**
     * Filter events with messages (reg. expressions) defined in the array.
     */
    public static Predicate<Event> ofMessages(String... messagesRegex) {
        return event -> {
            for (String regex : messagesRegex) {
                if (event.getMessage().matches(regex)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Filter events with involved object name defined in the array of reg. expressions.
     */
    public static Predicate<Event> ofObjNames(String... regexNames) {
        return event -> {
            for (String regexName : regexNames) {
                if (event.getInvolvedObject().getName().matches(regexName)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Filter events that are last seen in any if given time windows.
     * A structure of the array should be: [from date], [until date], [from date] [until date],...
     * Event needs to be seen strictly after {@code from date} and before or at the same time as {@code until date}.
     *
     * {@link ZonedDateTime} is used because a OpenShift cluster is distributed and time is provided in
     * {@link java.time.format.DateTimeFormatter#ISO_DATE_TIME}
     * format that consider time zones. Therefore wee need to compare it against {@link ZonedDateTime#now()} (for example)
     *
     * @see ZonedDateTime
     */
    public static Predicate<Event> inOneOfTimeWindows(ZonedDateTime... dates) {
        return event -> {
            if (event.getLastTimestamp() != null) {
                ZonedDateTime eventDate = EventHelper.timestampToZonedDateTime(event.getLastTimestamp());
                for (int i = 0; i < dates.length - 1; i += 2) {
                    if (eventDate.isAfter(dates[i])
                            && (eventDate.compareTo(dates[i + 1]) == 0 || eventDate.isBefore(dates[i + 1]))) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    /**
     * Filter events that are last seen strictly after the date.
     */
    public static Predicate<Event> after(ZonedDateTime date) {
        return e -> e.getLastTimestamp() != null && EventHelper.timestampToZonedDateTime(e.getLastTimestamp()).isAfter(date);
    }

    /**
     * Filter events with involved object kind defined in the array (case insensitive).
     */
    public static Predicate<Event> ofReasons(String... reasons) {
        return event -> isStrInArrayCaseInsensitive(event.getReason(), reasons);
    }

    private static boolean isStrInArrayCaseInsensitive(String str, String... array) {
        String field = str.toLowerCase();
        for (String item : array) {
            if (field.equals(item.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
