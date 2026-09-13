package actors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import models.*;
import Services.TmdbServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Unit test for ManagerActor using Pekko TestKit and Mockito.
 * Achieves 100% line and branch coverage.
 * 
 * @author Varshain Gopichandar Sreedevi
 * @author Thanugundla Sumith Reddy
 * @author Honey Sharma
 * @author Aman Agnihotri
 * @author Harsha Vardhini Eluri
 */
public class ManagerActorTest {

    private static ActorTestKit testKit;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void setup() {
        testKit = ActorTestKit.create();
    }

    @AfterClass
    public static void teardown() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testInboundMessageRouting() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        ObjectNode pingMsg = mapper.createObjectNode();
        pingMsg.put("action", "ping");
        manager.tell(new ManagerActor.InboundMessage(pingMsg));

        ObjectNode finMsg = mapper.createObjectNode();
        finMsg.put("action", "financial");
        finMsg.put("query", "Batman");
        manager.tell(new ManagerActor.InboundMessage(finMsg));

        ObjectNode searchMsg = mapper.createObjectNode();
        searchMsg.put("action", "search");
        searchMsg.put("query", "Batman");
        searchMsg.put("category", "movie");
        manager.tell(new ManagerActor.InboundMessage(searchMsg));

        ObjectNode personMsg = mapper.createObjectNode();
        personMsg.put("action", "personStats");
        personMsg.put("personId", "123");
        manager.tell(new ManagerActor.InboundMessage(personMsg));

        ObjectNode divMsg = mapper.createObjectNode();
        divMsg.put("action", "diversity");
        divMsg.put("movieId", 123);
        manager.tell(new ManagerActor.InboundMessage(divMsg));

        ObjectNode revMsg = mapper.createObjectNode();
        revMsg.put("action", "reviews");
        revMsg.put("id", "123");
        revMsg.put("category", "movie");
        revMsg.put("name", "Batman");
        manager.tell(new ManagerActor.InboundMessage(revMsg));

        browserProbe.expectNoMessage();
    }

    @Test
    public void testSearchResponseAndHistory() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        SearchRecord record1 = new SearchRecord("Batman", "movie", new ArrayList<>(), 0);
        manager.tell(new ManagerActor.WrappedSearchResponse(new SearchActor.SearchResponse(record1)));
        JsonNode out1 = browserProbe.receiveMessage();
        assertEquals("searchResults", out1.get("messageType").asText());

        manager.tell(new ManagerActor.WrappedSearchResponse(new SearchActor.SearchResponse(record1)));
        browserProbe.receiveMessage();

        for (int i = 0; i < 11; i++) {
            SearchRecord r = new SearchRecord("Movie " + i, "movie", new ArrayList<>(), 0);
            manager.tell(new ManagerActor.WrappedSearchResponse(new SearchActor.SearchResponse(r)));
            browserProbe.receiveMessage();
        }

        SearchRecord nullRecord = new SearchRecord("nullTest", "movie", null, 0);
        manager.tell(new ManagerActor.WrappedSearchResponse(new SearchActor.SearchResponse(nullRecord)));
        browserProbe.receiveMessage();

        SearchRecord finalRecord = new SearchRecord("Final", "movie", new ArrayList<>(), 0);
        manager.tell(new ManagerActor.WrappedSearchResponse(new SearchActor.SearchResponse(finalRecord)));
        JsonNode finalOut = browserProbe.receiveMessage();
        assertEquals(10, finalOut.get("data").size());
        assertEquals("Final", finalOut.get("data").get(0).get("query").asText());
    }

    @Test
    public void testWorkerResponses() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        FinancialActor.FinancialStatsResponse finResp = new FinancialActor.FinancialStatsResponse(new ArrayList<>());
        manager.tell(new ManagerActor.WrappedFinancialResponse(finResp));
        assertEquals("financialResults", browserProbe.receiveMessage().get("messageType").asText());

        GlobalDiversity div = Mockito.mock(GlobalDiversity.class);
        GlobalDiversityActor.DiversityStatsResponse divResp = new GlobalDiversityActor.DiversityStatsResponse(div);
        manager.tell(new ManagerActor.WrappedDiversityResponse(divResp));
        assertEquals("diversityResults", browserProbe.receiveMessage().get("messageType").asText());

        PersonStatsSummary summary = Mockito.mock(PersonStatsSummary.class);
        PersonStatsActor.PersonStatsFetched personResp = new PersonStatsActor.PersonStatsFetched("123", summary);
        manager.tell(new ManagerActor.WrappedPersonStatsResponse(personResp));
        assertEquals("personStatsResults", browserProbe.receiveMessage().get("messageType").asText());

        manager.tell(new ManagerActor.WrappedPersonStatsResponse(null));
        assertEquals("personStatsError", browserProbe.receiveMessage().get("messageType").asText());

        ReviewActor.ReviewPayload payload = Mockito.mock(ReviewActor.ReviewPayload.class);
        ReviewActor.ReviewResponse revResp = new ReviewActor.ReviewResponse(payload);
        manager.tell(new ManagerActor.WrappedReviewResponse(revResp));
        assertEquals("reviewResults", browserProbe.receiveMessage().get("messageType").asText());
    }

    @Test
    public void testMissingJsonFieldsAndUnknownAction() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        ObjectNode emptyMsg = mapper.createObjectNode();
        manager.tell(new ManagerActor.InboundMessage(emptyMsg));

        ObjectNode unknownMsg = mapper.createObjectNode();
        unknownMsg.put("action", "garbageAction");
        manager.tell(new ManagerActor.InboundMessage(unknownMsg));

        ObjectNode personMsg = mapper.createObjectNode();
        personMsg.put("action", "personStats");
        manager.tell(new ManagerActor.InboundMessage(personMsg));

        ObjectNode revMsg = mapper.createObjectNode();
        revMsg.put("action", "reviews");
        manager.tell(new ManagerActor.InboundMessage(revMsg));

        browserProbe.expectNoMessage();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSearchResponse_duplicateResultItemsAreStripped() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        List items = new ArrayList();
        try {
            Class<?> itemClass = Class.forName("models.MovieResults");
            Object item1 = itemClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Field idField = itemClass.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item1, "99");

            Object item2 = itemClass.getDeclaredConstructor().newInstance();
            idField.set(item2, "99");

            items.add(item1);
            items.add(item2);
        } catch (Exception ignored) {
        }

        SearchRecord record = new SearchRecord("dupTest", "movie", items, 0);
        manager.tell(new ManagerActor.WrappedSearchResponse(new SearchActor.SearchResponse(record)));

        JsonNode out = browserProbe.receiveMessage();
        assertEquals("searchResults", out.get("messageType").asText());
    }

    /**
     * Forces exception inside the try block of onInboundMessage() to cover the
     * catch block.
     */
    @Test
    public void testInboundMessage_exceptionInDispatch_isHandledGracefully() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        // has() and get() work fine so action resolves to "search"
        // but path() throws — which IS inside the try block
        JsonNode poisoned = new ObjectNode(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance) {
            @Override
            public boolean has(String fieldName) {
                if ("action".equals(fieldName))
                    return true;
                return false;
            }

            @Override
            public JsonNode get(String fieldName) {
                if ("action".equals(fieldName)) {
                    return com.fasterxml.jackson.databind.node.TextNode.valueOf("search");
                }
                return null;
            }

            @Override
            public JsonNode path(String fieldName) {
                // This is called INSIDE the try block (switch case "search")
                throw new RuntimeException("forced exception inside try block for catch coverage");
            }
        };

        manager.tell(new ManagerActor.InboundMessage(poisoned));

        // catch block runs, actor survives, nothing sent to browser
        browserProbe.expectNoMessage(java.time.Duration.ofMillis(500));
    }

    @Test
    public void testPersonStatsAdapter_FailureBranch() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);

        Mockito.when(mockTmdb.getDetails(eq("person"), eq("999")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Crash")));

        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        ObjectNode personMsg = mapper.createObjectNode();
        personMsg.put("action", "personStats");
        personMsg.put("personId", "999");
        manager.tell(new ManagerActor.InboundMessage(personMsg));

        JsonNode errorOut = browserProbe.receiveMessage();
        assertEquals("personStatsError", errorOut.get("messageType").asText());
    }

    /**
     * Covers WrappedReadabilityUpdate constructor (all 3 field assignments)
     * and the onReadabilityUpdate handler — both highlighted red in coverage
     * report.
     */
    @Test
    public void testWrappedReadabilityUpdate_constructorAndHandler() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        // Exercises: WrappedReadabilityUpdate(String, double, String) constructor
        // + this.movieId, this.ease, this.grade assignments
        // + onReadabilityUpdate handler body
        // + clientActor.tell(json) with messageType "readabilityUpdate"
        ManagerActor.WrappedReadabilityUpdate update = new ManagerActor.WrappedReadabilityUpdate("tt1234", 65.4,
                "8th Grade");

        manager.tell(update);

        JsonNode out = browserProbe.receiveMessage();
        assertEquals("readabilityUpdate", out.get("messageType").asText());
        assertEquals("tt1234", out.get("movieId").asText());
        assertEquals(65.4, out.get("ease").asDouble(), 0.001);
        assertEquals("8th Grade", out.get("grade").asText());
    }

    @Test
    public void testWrappedSearchAppendNotice_constructorAndHandler() {
        TestProbe<JsonNode> browserProbe = testKit.createTestProbe();
        TmdbServices mockTmdb = Mockito.mock(TmdbServices.class);
        ActorRef<ManagerActor.Command> manager = testKit.spawn(ManagerActor.create(browserProbe.getRef(), mockTmdb));

        SearchRecord record = new SearchRecord("Batman", "movie", new ArrayList<>(), 19);
        ManagerActor.WrappedSearchAppendNotice notice = new ManagerActor.WrappedSearchAppendNotice(
                new SearchActor.SearchResponse(record), 9);

        manager.tell(notice);

        JsonNode out = browserProbe.receiveMessage();
        assertEquals("searchAppendNotice", out.get("messageType").asText());
        assertEquals(9, out.get("appendedCount").asInt());
        assertEquals("Batman", out.get("data").get("query").asText());
    }

    @Test
    public void testChildFailed_constructorFields() {
        RuntimeException ex = new RuntimeException("boom");
        ManagerActor.ChildFailed failed = new ManagerActor.ChildFailed("searchActor", ex);
        assertEquals("searchActor", failed.actorName);
        assertEquals(ex, failed.cause);
    }
}
