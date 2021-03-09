package cz.xtf.core.waiting.failfast;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cz.xtf.core.event.EventFilters;
import cz.xtf.core.openshift.OpenShift;
import io.fabric8.kubernetes.api.model.Event;

/**
 * Builder for creating fail fast checks for event
 * You can filter events by name, obj kind, obj name, time,...
 * <p>
 * Builds {@link WatchedResourcesSupplier} and provides reason when a check fails - list of filtered events and filter itself
 */
public class EventFailFastCheckBuilder {

    private final FailFastBuilder failFastBuilder;
    private String[] names = null;
    private ZonedDateTime after;
    private String[] reasons;
    private String[] messages;
    private String[] types;
    private String[] kinds;

    EventFailFastCheckBuilder(FailFastBuilder failFastBuilder) {
        this.failFastBuilder = failFastBuilder;
    }

    /**
     * Regexes to match event involved object name.
     */
    public EventFailFastCheckBuilder ofNames(String... name) {
        this.names = name;
        return this;
    }

    /**
     * Array of demanded reasons of events (case in-sensitive). One of them must be equal.
     */
    public EventFailFastCheckBuilder ofReasons(String... reasons) {
        this.reasons = reasons;
        return this;
    }

    /**
     * Array of demanded types of events (case in-sensitive). One of them must be equal.
     * For example: {@code Warning}, {@code Normal}, ...
     */
    public EventFailFastCheckBuilder ofTypes(String... types) {
        this.types = types;
        return this;
    }

    /**
     * Array of demanded object kinds of events (case in-sensitive). One of them must be equal.
     * For example: {@code persistentvolume}, {@code pod}, ...
     */
    public EventFailFastCheckBuilder ofKinds(String... kinds) {
        this.kinds = kinds;
        return this;
    }

    /**
     * Regexes for demanded messages. One of them must match.
     *
     * @param messages
     * @return
     */
    public EventFailFastCheckBuilder ofMessages(String... messages) {
        this.messages = messages;
        return this;
    }

    /**
     * If at least one event exist (after filtration), final function returns true.
     */
    public FailFastBuilder atLeastOneExists() {
        // function is invoked every time...everytime we get events and filter them
        failFastBuilder.addFailFastCheck(new WatchedResourcesSupplier<>(
                this::getFilterEventList,
                events -> !events.isEmpty(),
                events -> failFastReason(events, "at least one exists")));

        return failFastBuilder;
    }

    private String failFastReason(List<Event> eventList, String condition) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Following events match condition: <").append(condition).append(">\n");
        eventList.forEach(e -> stringBuilder
                .append("\t")
                .append(e.getLastTimestamp())
                .append("\t")
                .append(e.getInvolvedObject().getKind())
                .append("/")
                .append(e.getInvolvedObject().getName())
                .append("\t")
                .append(e.getMessage())
                .append("\n"));
        stringBuilder.append("Filter:");
        if (kinds != null) {
            stringBuilder.append("\t obj kinds: ").append(Arrays.toString(kinds)).append("\n");
        }
        if (names != null) {
            stringBuilder.append("\t obj names: ").append(Arrays.toString(names)).append("\n");
        }
        if (reasons != null) {
            stringBuilder.append("\t event reasons: ").append(Arrays.toString(reasons)).append("\n");
        }
        if (messages != null) {
            stringBuilder.append("\t messages: ").append(Arrays.toString(messages)).append("\n");
        }
        if (types != null) {
            stringBuilder.append("\t event types: ").append(Arrays.toString(types)).append("\n");
        }
        if (after != null) {
            stringBuilder.append("\t after: ").append(after.toString()).append("\n");
        }
        return stringBuilder.toString();

    }

    /**
     * Consider event after certain time.
     */
    public EventFailFastCheckBuilder after(ZonedDateTime after) {
        this.after = after;
        return this;
    }

    private List<Event> getFilterEventList() {
        Stream<Event> stream = getEventsForAllNamespaces().stream();
        if (names != null) {
            stream = stream.filter(EventFilters.ofObjNames(names));
        }
        if (after != null) {
            stream = stream.filter(EventFilters.after(after));
        }
        if (reasons != null) {
            stream = stream.filter(EventFilters.ofReasons(reasons));
        }
        if (messages != null) {
            stream = stream.filter(EventFilters.ofMessages(messages));
        }
        if (types != null) {
            stream = stream.filter(EventFilters.ofEventTypes(types));
        }
        if (kinds != null) {
            stream = stream.filter(EventFilters.ofObjKinds(kinds));
        }
        return stream.collect(Collectors.toList());
    }

    private List<Event> getEventsForAllNamespaces() {
        List<Event> events = null;

        for (OpenShift openShift : failFastBuilder.getOpenshifts()) {
            if (events == null) {
                events = openShift.getEvents();
            } else {
                events.addAll(openShift.getEvents());
            }
        }
        return events;
    }
}
