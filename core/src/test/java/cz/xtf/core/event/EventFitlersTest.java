package cz.xtf.core.event;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.xtf.core.event.helpers.EventHelper;
import io.fabric8.kubernetes.api.model.Event;

public class EventFitlersTest {

    private static String eventTemplate = "        {\n"
            + "            \"apiVersion\": \"v1\",\n"
            + "            \"count\": 1,\n"
            + "            \"eventTime\": null,\n"
            + "            \"firstTimestamp\": \"2020-05-22T06:17:43Z\",\n"
            + "            \"involvedObject\": {\n"
            + "                \"apiVersion\": \"v1\",\n"
            + "                \"fieldPath\": \"spec.containers{pv-recycler}\",\n"
            + "                \"kind\": \"%s\",\n" // PLACEHOLDER HERE
            + "                \"name\": \"%s\",\n" // PLACEHOLDER HERE
            + "                \"namespace\": \"default\",\n"
            + "                \"resourceVersion\": \"7611264\",\n"
            + "                \"uid\": \"cef5e166-cc1f-4403-8e11-026d6c050378\"\n"
            + "            },\n"
            + "            \"kind\": \"Event\",\n"
            + "            \"lastTimestamp\": \"%s\",\n" // PLACEHOLDER HERE
            + "            \"message\": \"%s\",\n" // PLACEHOLDER HERE
            + "            \"metadata\": {\n"
            + "                \"creationTimestamp\": \"2020-05-22T06:17:43Z\",\n"
            + "                \"name\": \"recycler-for-pv0005.1611453afcf35b01\",\n"
            + "                \"namespace\": \"default\",\n"
            + "                \"resourceVersion\": \"7611298\",\n"
            + "                \"selfLink\": \"/api/v1/namespaces/default/events/recycler-for-pv0005.1611453afcf35b01\",\n"
            + "                \"uid\": \"3769b0e9-f035-418c-b8ad-07419c06de06\"\n"
            + "            },\n"
            + "            \"reason\": \"%s\",\n" // PLACEHOLDER HERE
            + "            \"reportingComponent\": \"\",\n"
            + "            \"reportingInstance\": \"\",\n"
            + "            \"source\": {\n"
            + "                \"component\": \"kubelet\",\n"
            + "                \"host\": \"eapqe-007-srtg-rmf59-worker-znzs2\"\n"
            + "            },\n"
            + "            \"type\": \"%s\"\n" // PLACEHOLDER HERE
            + "        }\n";

    private static Event event(String lastTimestamp, String involvedObjectkind, String involvedObjectName, String reason,
            String type, String message) throws JsonProcessingException {
        return new ObjectMapper().readValue(
                String.format(eventTemplate, involvedObjectkind, involvedObjectName, lastTimestamp, message, reason, type),
                Event.class);
    }

    @Test
    public void messageFiltrationTest() throws JsonProcessingException {
        List<Event> events = Arrays.asList(
                event("2020-01-01T00:00:00Z", "Pod", "1", "Created", "Normal", "message2"),
                event("2020-01-01T00:00:00Z", "Pod", "2", "Created", "Normal", "keyword abc"),
                event("2020-01-01T00:00:00Z", "Pod", "3", "Created", "Normal", "random keyword random"),
                event("2020-01-01T00:00:00Z", "Pod", "4", "Created", "Normal", "random another random"),
                event("2020-01-01T00:00:00Z", "Pod", "5", "Created", "Normal", "message1"));

        List<Event> filtered = events.stream()
                .filter(EventFilters.ofMessages("keyword.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("2", filtered.get(0).getInvolvedObject().getName());

        filtered = events.stream()
                .filter(EventFilters.ofMessages(".*keyword.*", ".*another.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(3, filtered.size());
        List<String> names = filtered.stream()
                .map(event -> event.getInvolvedObject().getName())
                .collect(Collectors.toList());
        Assertions.assertTrue(names.contains("2"));
        Assertions.assertTrue(names.contains("3"));
        Assertions.assertTrue(names.contains("4"));

        filtered = events.stream()
                .filter(EventFilters.ofMessages("nonexisting.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(0, filtered.size());
    }

    @Test
    public void reasonFiltrationTest() throws JsonProcessingException {
        List<Event> events = Arrays.asList(
                event("2020-01-01T00:00:00Z", "Pod", "1", "FailedToCreateEndpoint", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "2", "RecyclerPod", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "3", "VolumeRecycled", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "4", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "5", "VolumeRecycled", "Normal", "message"));

        List<Event> filtered = events.stream()
                .filter(EventFilters.ofReasons("created"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("4", filtered.get(0).getInvolvedObject().getName());

        filtered = events.stream()
                .filter(EventFilters.ofReasons("recyclerPod", "VolumeRecycled"))
                .collect(Collectors.toList());
        Assertions.assertEquals(3, filtered.size());
        List<String> names = filtered.stream()
                .map(event -> event.getInvolvedObject().getName())
                .collect(Collectors.toList());
        Assertions.assertTrue(names.contains("2"));
        Assertions.assertTrue(names.contains("3"));
        Assertions.assertTrue(names.contains("5"));

        filtered = events.stream()
                .filter(EventFilters.ofReasons("nonexisting.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(0, filtered.size());
    }

    @Test
    public void objKindFiltrationTest() throws JsonProcessingException {
        List<Event> events = Arrays.asList(
                event("2020-01-01T00:00:00Z", "persistentvolume", "1", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "2", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "enpoints", "3", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "4", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "weirdKind", "5", "Created", "Normal", "message"));

        List<Event> filtered = events.stream()
                .filter(EventFilters.ofObjKinds("weirdkind"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("5", filtered.get(0).getInvolvedObject().getName());

        filtered = events.stream()
                .filter(EventFilters.ofObjKinds("persistentvolume", "pod"))
                .collect(Collectors.toList());
        Assertions.assertEquals(3, filtered.size());
        List<String> names = filtered.stream()
                .map(event -> event.getInvolvedObject().getName())
                .collect(Collectors.toList());
        Assertions.assertTrue(names.contains("1"));
        Assertions.assertTrue(names.contains("2"));
        Assertions.assertTrue(names.contains("4"));

        filtered = events.stream()
                .filter(EventFilters.ofObjKinds("nonexisting.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(0, filtered.size());
    }

    @Test
    public void typeFiltrationTest() throws JsonProcessingException {
        List<Event> events = Arrays.asList(
                event("2020-01-01T00:00:00Z", "Pod", "1", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "2", "Created", "Warning", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "3", "Created", "Error", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "4", "Created", "Failure", "message"),
                event("2020-01-01T00:00:00Z", "Pod", "5", "Created", "Normal", "message"));

        List<Event> filtered = events.stream()
                .filter(EventFilters.ofEventTypes("error"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("3", filtered.get(0).getInvolvedObject().getName());

        filtered = events.stream()
                .filter(EventFilters.ofEventTypes("normal", "warning"))
                .collect(Collectors.toList());
        Assertions.assertEquals(3, filtered.size());
        List<String> names = filtered.stream()
                .map(event -> event.getInvolvedObject().getName())
                .collect(Collectors.toList());
        Assertions.assertTrue(names.contains("1"));
        Assertions.assertTrue(names.contains("2"));
        Assertions.assertTrue(names.contains("5"));

        filtered = events.stream()
                .filter(EventFilters.ofEventTypes("nonexisting.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(0, filtered.size());
    }

    @Test
    public void objNameFiltrationTest() throws JsonProcessingException {
        List<Event> events = Arrays.asList(
                event("2020-01-01T00:00:00Z", "1", "oneapp-deploy", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "2", "two", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "3", "oneapp-runner", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "4", "three", "Created", "Normal", "message"),
                event("2020-01-01T00:00:00Z", "5", "four", "Created", "Normal", "message"));

        List<Event> filtered = events.stream()
                .filter(EventFilters.ofObjNames("two"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("2", filtered.get(0).getInvolvedObject().getKind());

        filtered = events.stream()
                .filter(EventFilters.ofObjNames("oneapp.*", "three.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(3, filtered.size());
        List<String> names = filtered.stream()
                .map(event -> event.getInvolvedObject().getKind())
                .collect(Collectors.toList());
        Assertions.assertTrue(names.contains("1"));
        Assertions.assertTrue(names.contains("3"));
        Assertions.assertTrue(names.contains("4"));

        filtered = events.stream()
                .filter(EventFilters.ofEventTypes("nonexisting.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(0, filtered.size());
    }

    @Test
    public void timeFiltrationTest() throws JsonProcessingException {
        List<Event> events = Arrays.asList(
                event("2020-01-01T00:00:00Z", "Pod", "1", "Created", "Normal", "message"),
                event("2020-01-01T01:00:00Z", "Pod", "2", "Created", "Normal", "message"),
                event("2020-01-01T01:10:00Z", "Pod", "3", "Created", "Normal", "message"),
                event("2020-01-01T02:00:00Z", "Pod", "4", "Created", "Normal", "message"),
                event("2020-01-01T03:00:00Z", "Pod", "5", "Created", "Normal", "message"),
                event("2020-01-01T03:33:00Z", "Pod", "6", "Created", "Normal", "message"),
                event("2020-01-01T04:00:00Z", "Pod", "7", "Created", "Normal", "message"));

        List<Event> filtered = events.stream()
                .filter(EventFilters.inOneOfTimeWindows(
                        EventHelper.timestampToZonedDateTime("2020-01-01T01:59:00Z"),
                        EventHelper.timestampToZonedDateTime("2020-01-01T02:59:59Z"),
                        EventHelper.timestampToZonedDateTime("2020-01-01T04:00:01Z"),
                        EventHelper.timestampToZonedDateTime("2021-01-01T03:00:01Z")))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("4", filtered.get(0).getInvolvedObject().getName());

        filtered = events.stream()
                .filter(EventFilters.after(
                        EventHelper.timestampToZonedDateTime("2020-01-01T03:33:00Z")))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("7", filtered.get(0).getInvolvedObject().getName());

        filtered = events.stream()
                .filter(EventFilters.inOneOfTimeWindows(
                        EventHelper.timestampToZonedDateTime("2020-01-01T00:59:00Z"),
                        EventHelper.timestampToZonedDateTime("2020-01-01T02:59:59Z"),
                        EventHelper.timestampToZonedDateTime("2020-01-01T03:00:01Z"),
                        EventHelper.timestampToZonedDateTime("2021-01-01T03:00:01Z")))
                .collect(Collectors.toList());
        Assertions.assertEquals(5, filtered.size());
        List<String> names = filtered.stream()
                .map(event -> event.getInvolvedObject().getName())
                .collect(Collectors.toList());
        Assertions.assertTrue(names.contains("2"));
        Assertions.assertTrue(names.contains("3"));
        Assertions.assertTrue(names.contains("4"));
        Assertions.assertTrue(names.contains("6"));
        Assertions.assertTrue(names.contains("7"));

        filtered = events.stream()
                .filter(EventFilters.inOneOfTimeWindows(EventHelper.timestampToZonedDateTime("2021-01-01T03:00:01Z"),
                        EventHelper.timestampToZonedDateTime("2021-01-01T03:00:01Z")))
                .collect(Collectors.toList());
        Assertions.assertEquals(0, filtered.size());
    }

    @Test
    public void multipleFiltrationTest() throws JsonProcessingException {
        List<Event> events = Arrays.asList(
                event("2020-01-01T00:00:00Z", "Pod", "1", "Created", "Normal", "message"),
                event("2020-01-01T01:00:00Z", "Pod", "2", "Created", "Normal", "message"),
                event("2020-01-01T02:00:00Z", "deploymentconfig", "no-app-build", "error", "Normal", "message"),
                event("2020-01-01T03:00:00Z", "deploymentconfig", "no-app-build", "Created", "Normal", "keyword"),
                event("2020-01-01T04:00:00Z", "Pod", "5", "Created", "Normal", "message"),
                event("2020-01-01T05:00:00Z", "Pod", "6", "Created", "Normal", "message"),
                event("2020-01-01T06:00:00Z", "Pod", "7", "Created", "Normal", "message"),
                // looking for this one
                event("2020-01-01T07:00:00Z", "deploymentconfig", "myapp-deploy", "Created", "Normal", "buzz keyword noise"),
                event("2020-01-01T07:20:00Z", "Pod", "myapp-xyz", "Created", "Normal", "message"),
                event("2020-01-01T17:22:00Z", "deploymentconfig", "7", "Created", "Normal", "message"),
                event("2020-01-01T11:00:00Z", "Pod", "7", "Created", "Normal", "message"),
                event("2020-01-01T12:00:00Z", "deploymentconfig", "7", "Created", "Normal", "message"),
                event("2020-01-01T13:00:00Z", "deploymentconfig", "7", "Created", "Normal", "message"),
                event("2020-01-01T14:00:00Z", "Pod", "7", "Created", "Normal", "message"),
                //looking for this one
                event("2020-01-01T15:00:00Z", "Pod", "myapp-run", "failed", "Error", "silence foobar noise"),
                event("2020-01-01T15:10:00Z", "Pod", "7", "Created", "Normal", "message"));

        List<Event> filtered = events.stream()
                .filter(EventFilters.inOneOfTimeWindows(
                        EventHelper.timestampToZonedDateTime("2020-01-01T06:30:00Z"),
                        EventHelper.timestampToZonedDateTime("2020-01-01T07:30:00Z"),
                        EventHelper.timestampToZonedDateTime("2020-01-01T14:30:00Z"),
                        EventHelper.timestampToZonedDateTime("2020-01-01T15:30:00Z")))
                .filter(EventFilters.ofEventTypes("normal", "error"))
                .filter(EventFilters.ofMessages(".*keyword.*", ".*foobar.*"))
                .filter(EventFilters.ofReasons("created", "failed"))
                .filter(EventFilters.ofObjKinds("pod", "deploymentconfig"))
                .filter(EventFilters.ofObjNames("myapp.*"))
                .collect(Collectors.toList());
        Assertions.assertEquals(2, filtered.size());
        List<String> names = filtered.stream()
                .map(event -> event.getInvolvedObject().getName())
                .collect(Collectors.toList());
        Assertions.assertTrue(names.contains("myapp-deploy"));
        Assertions.assertTrue(names.contains("myapp-run"));
    }
}
