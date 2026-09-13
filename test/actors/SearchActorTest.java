package actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import models.SearchRecord;
import models.MovieResults;
import Services.TmdbServices;

import java.util.concurrent.CompletableFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Ultimate Unit test for the SearchActor using Pekko TestKit.
 * 
 * @author Thanugundla Sumith Reddy
 */
public class SearchActorTest {

        private static final ActorTestKit testKit = ActorTestKit.create();

        /**
         * Cleans up the ActorTestKit after all tests are executed.
         * Ensures proper shutdown of the actor system.
         *
         * @author Thanugundla Sumith Reddy
         */
        @AfterClass
        public static void cleanup() {
                testKit.shutdownTestKit();
        }

        /**
         * Tests the PerformSearch message handling of the SearchActor.
         * <p>
         * This test:
         * <ul>
         * <li>Mocks TMDb API search response</li>
         * <li>Builds a realistic JSON payload</li>
         * <li>Verifies parsing into SearchRecord</li>
         * <li>Ensures at least one result is returned</li>
         * </ul>
         *
         * @author Thanugundla Sumith Reddy
         */
        @Test
        public void testPerformSearch() {

                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
                ObjectMapper mapper = new ObjectMapper();

                ObjectNode fakeJson = mapper.createObjectNode();
                fakeJson.put("total_results", 1);
                ArrayNode results = mapper.createArrayNode();

                ObjectNode movie = mapper.createObjectNode();
                movie.put("id", 123);
                movie.put("title", "Batman");
                movie.put("overview", "A great movie");
                movie.put("release_date", "2022-01-01");
                movie.put("popularity", 100.0);
                movie.put("vote_average", 8.5);
                movie.put("vote_count", 1000);
                movie.set("genre_ids", mapper.createArrayNode());

                results.add(movie);
                fakeJson.set("results", results);

                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman")))
                                .thenReturn(CompletableFuture.completedFuture(fakeJson));
                Mockito.when(mockTmdbService.getGenres(any(), any())).thenReturn("Action");

                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));

                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                searchActor.tell(
                                new SearchActor.PerformSearch("Batman", "movie", probe.getRef()));

                ManagerActor.WrappedSearchResponse wrapped = probe
                                .expectMessageClass(ManagerActor.WrappedSearchResponse.class);

                SearchActor.SearchResponse response = wrapped.response;

                assertNotNull(response);
                assertEquals("Batman", response.record.query);
                assertEquals(1, response.record.getResults().size());
        }

        /**
         * Tests edge cases for WrappedSearchResponse handling.
         * <p>
         * Covers:
         * <ul>
         * <li>Null SearchRecord</li>
         * <li>SearchRecord with null results list</li>
         * </ul>
         * Uses reflection to access private inner class and ensure
         * full branch coverage.
         *
         * @throws Exception if reflection fails
         *
         * @author Thanugundla Sumith Reddy
         */
        @Test
        public void testWrappedResponse_NullBranches() throws Exception {

                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);

                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));

                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                Class<?> wrappedResponseClass = Class.forName("actors.SearchActor$WrappedSearchResponse");

                java.lang.reflect.Constructor<?> constructor = wrappedResponseClass.getDeclaredConstructor(
                                models.SearchRecord.class,
                                ActorRef.class);

                constructor.setAccessible(true);

                // --- BRANCH 1: Null record ---
                SearchActor.Command nullRecordMsg = (SearchActor.Command) constructor.newInstance(null, probe.getRef());

                searchActor.tell(nullRecordMsg);

                ManagerActor.WrappedSearchResponse wrapped1 = probe
                                .expectMessageClass(ManagerActor.WrappedSearchResponse.class);

                assertNull(wrapped1.response.record);

                // --- BRANCH 2: Record exists but results are null ---
                SearchRecord nullResultsRecord = new SearchRecord("Batman", "movie", null, 0);

                SearchActor.Command nullListMsg = (SearchActor.Command) constructor.newInstance(nullResultsRecord,
                                probe.getRef());

                searchActor.tell(nullListMsg);

                ManagerActor.WrappedSearchResponse wrapped2 = probe
                                .expectMessageClass(ManagerActor.WrappedSearchResponse.class);

                assertNotNull(wrapped2.response.record);
                assertNull(wrapped2.response.record.getResults());
        }

        /**
         * Tests WrappedReadabilityResponse handling and onReadabilityResult method.
         * <p>
         * This test uses reflection to directly instantiate the private inner class
         * (bypassing the real ReadabilityActor response flow) and sends it to
         * the SearchActor. This covers:
         * <ul>
         * <li>WrappedReadabilityResponse constructor (the three assignment lines)</li>
         * <li>onReadabilityResult method (the manager.tell call)</li>
         * </ul>
         * Exactly targets the red-highlighted uncovered code while leaving
         * all previously 100% covered tests untouched.
         *
         * @throws Exception if reflection fails
         *
         * @author Thanugundla Sumith Reddy
         */
        @Test
        public void testWrappedReadabilityResponse() throws Exception {

                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);

                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));

                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                Class<?> wrappedReadabilityClass = Class.forName("actors.SearchActor$WrappedReadabilityResponse");

                java.lang.reflect.Constructor<?> constructor = wrappedReadabilityClass.getDeclaredConstructor(
                                ReadabilityActor.ReadabilityResponse.class,
                                String.class,
                                ActorRef.class);

                constructor.setAccessible(true);

                // Mock the inner response object (non-null so .ease / .grade access does not
                // NPE)
                ReadabilityActor.ReadabilityResponse mockResponse = Mockito
                                .mock(ReadabilityActor.ReadabilityResponse.class);

                String movieId = "123";

                SearchActor.Command readabilityMsg = (SearchActor.Command) constructor.newInstance(
                                mockResponse, movieId, probe.getRef());

                searchActor.tell(readabilityMsg);

                // The onReadabilityResult handler fires and sends WrappedReadabilityUpdate to
                // the probe
                ManagerActor.WrappedReadabilityUpdate update = probe
                                .expectMessageClass(ManagerActor.WrappedReadabilityUpdate.class);

                assertNotNull(update);
        }

        @Test
        public void testPollingAppendsAndStopsOnEmptyPage() throws Exception {
                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
                ObjectMapper mapper = new ObjectMapper();

                ObjectNode initialJson = mapper.createObjectNode();
                initialJson.put("total_results", 19);
                ArrayNode initialResults = mapper.createArrayNode();
                ObjectNode movie1 = mapper.createObjectNode();
                movie1.put("id", "1");
                movie1.put("title", "Batman One");
                movie1.put("overview", "Overview 1");
                movie1.put("release_date", "2022-01-01");
                movie1.put("popularity", 100.0);
                movie1.put("vote_average", 8.0);
                movie1.set("genre_ids", mapper.createArrayNode());
                initialResults.add(movie1);
                initialJson.set("results", initialResults);

                ObjectNode pageOnePollingJson = mapper.createObjectNode();
                pageOnePollingJson.put("total_results", 19);
                ArrayNode pageOnePollingResults = mapper.createArrayNode();
                pageOnePollingResults.add(movie1);
                ObjectNode movie2 = mapper.createObjectNode();
                movie2.put("id", "2");
                movie2.put("title", "Batman Two");
                movie2.put("overview", "Overview 2");
                movie2.put("release_date", "2022-01-02");
                movie2.put("popularity", 90.0);
                movie2.put("vote_average", 7.8);
                movie2.set("genre_ids", mapper.createArrayNode());
                pageOnePollingResults.add(movie2);
                pageOnePollingJson.set("results", pageOnePollingResults);

                ObjectNode emptyPageJson = mapper.createObjectNode();
                emptyPageJson.put("total_results", 19);
                emptyPageJson.set("results", mapper.createArrayNode());

                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman")))
                                .thenReturn(CompletableFuture.completedFuture(initialJson));
                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman"), eq(1)))
                                .thenReturn(CompletableFuture.completedFuture(pageOnePollingJson));
                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman"), eq(2)))
                                .thenReturn(CompletableFuture.completedFuture(emptyPageJson));
                Mockito.when(mockTmdbService.getGenres(any(), any())).thenReturn("Action");

                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));
                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                searchActor.tell(new SearchActor.PerformSearch("Batman", "movie", probe.getRef()));
                probe.expectMessageClass(ManagerActor.WrappedSearchResponse.class);
                probe.expectMessageClass(ManagerActor.WrappedReadabilityUpdate.class);

                Class<?> checkClass = Class.forName("actors.SearchActor$CheckForMoreResults");
                java.lang.reflect.Constructor<?> checkCtor = checkClass.getDeclaredConstructor(String.class,
                                String.class, ActorRef.class);
                checkCtor.setAccessible(true);

                SearchActor.Command check1 = (SearchActor.Command) checkCtor.newInstance("Batman", "movie",
                                probe.getRef());
                searchActor.tell(check1);

                ManagerActor.WrappedSearchAppendNotice notice = probe
                                .expectMessageClass(ManagerActor.WrappedSearchAppendNotice.class);
                assertEquals(1, notice.appendedCount);
                assertNotNull(notice.response.record);
                assertTrue(notice.response.record.getResults().size() >= 2);

                SearchActor.Command check2 = (SearchActor.Command) checkCtor.newInstance("Batman", "movie",
                                probe.getRef());
                searchActor.tell(check2);
                probe.expectNoMessage(Duration.ofMillis(400));
        }

        @Test
        public void testCheckForMoreResults_IgnoresMismatchedQueryOrCategory() throws Exception {
                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
                ObjectMapper mapper = new ObjectMapper();

                ObjectNode initialJson = mapper.createObjectNode();
                initialJson.put("total_results", 1);
                ArrayNode initialResults = mapper.createArrayNode();
                ObjectNode movie = mapper.createObjectNode();
                movie.put("id", "1");
                movie.put("title", "Batman One");
                movie.put("overview", "Overview 1");
                movie.put("release_date", "2022-01-01");
                movie.put("popularity", 100.0);
                movie.put("vote_average", 8.0);
                movie.set("genre_ids", mapper.createArrayNode());
                initialResults.add(movie);
                initialJson.set("results", initialResults);

                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman")))
                                .thenReturn(CompletableFuture.completedFuture(initialJson));
                Mockito.when(mockTmdbService.getGenres(any(), any())).thenReturn("Action");

                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));
                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                searchActor.tell(new SearchActor.PerformSearch("Batman", "movie", probe.getRef()));
                probe.expectMessageClass(ManagerActor.WrappedSearchResponse.class);
                probe.expectMessageClass(ManagerActor.WrappedReadabilityUpdate.class);

                Class<?> checkClass = Class.forName("actors.SearchActor$CheckForMoreResults");
                java.lang.reflect.Constructor<?> checkCtor = checkClass.getDeclaredConstructor(String.class,
                                String.class, ActorRef.class);
                checkCtor.setAccessible(true);

                SearchActor.Command mismatched = (SearchActor.Command) checkCtor.newInstance("Iron Man", "movie",
                                probe.getRef());
                searchActor.tell(mismatched);

                SearchActor.Command mismatchedCategory = (SearchActor.Command) checkCtor.newInstance("Batman", "tv",
                                probe.getRef());
                searchActor.tell(mismatchedCategory);

                probe.expectNoMessage(Duration.ofMillis(300));
                Mockito.verify(mockTmdbService, Mockito.never()).search(eq("movie"), eq("Iron Man"), Mockito.anyInt());
        }

        @Test
        public void testWrappedSearchDelta_EarlyReturnGuards() throws Exception {
                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));
                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                Class<?> wrappedDeltaClass = Class.forName("actors.SearchActor$WrappedSearchDelta");
                java.lang.reflect.Constructor<?> deltaCtor = wrappedDeltaClass
                                .getDeclaredConstructor(models.SearchRecord.class, ActorRef.class);
                deltaCtor.setAccessible(true);

                // guard 1: record == null
                SearchActor.Command nullRecordDelta = (SearchActor.Command) deltaCtor.newInstance(null, probe.getRef());
                searchActor.tell(nullRecordDelta);

                // guard 2: record.getResults() == null
                SearchRecord nullResults = new SearchRecord("Batman", "movie", null, 0);
                SearchActor.Command nullResultsDelta = (SearchActor.Command) deltaCtor.newInstance(nullResults,
                                probe.getRef());
                searchActor.tell(nullResultsDelta);

                // guard 3: activeRecord == null while record/results exist
                List<MovieResults> oneResult = new ArrayList<>();
                oneResult.add(new MovieResults("1", "Batman One", "2022-01-01", "en",
                                100.0, 8.0, "Action", "Unknown", "N/A", null, null, "Overview 1"));
                SearchRecord noActiveRecordYet = new SearchRecord("Batman", "movie", oneResult, 1);
                SearchActor.Command activeNullDelta = (SearchActor.Command) deltaCtor.newInstance(noActiveRecordYet,
                                probe.getRef());
                searchActor.tell(activeNullDelta);

                probe.expectNoMessage(Duration.ofMillis(300));
        }

        @Test
        public void testWrappedSearchDelta_DuplicateAndAppendCapBranches() throws Exception {
                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
                ObjectMapper mapper = new ObjectMapper();

                ObjectNode initialJson = mapper.createObjectNode();
                initialJson.put("total_results", 1);
                ArrayNode initialResults = mapper.createArrayNode();
                ObjectNode movie = mapper.createObjectNode();
                movie.put("id", "1");
                movie.put("title", "Batman One");
                movie.put("overview", "Overview 1");
                movie.put("release_date", "2022-01-01");
                movie.put("popularity", 100.0);
                movie.put("vote_average", 8.0);
                movie.set("genre_ids", mapper.createArrayNode());
                initialResults.add(movie);
                initialJson.set("results", initialResults);

                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman")))
                                .thenReturn(CompletableFuture.completedFuture(initialJson));
                Mockito.when(mockTmdbService.getGenres(any(), any())).thenReturn("Action");

                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));
                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                searchActor.tell(new SearchActor.PerformSearch("Batman", "movie", probe.getRef()));
                probe.expectMessageClass(ManagerActor.WrappedSearchResponse.class);
                probe.expectMessageClass(ManagerActor.WrappedReadabilityUpdate.class);

                Class<?> wrappedDeltaClass = Class.forName("actors.SearchActor$WrappedSearchDelta");
                java.lang.reflect.Constructor<?> deltaCtor = wrappedDeltaClass
                                .getDeclaredConstructor(models.SearchRecord.class, ActorRef.class);
                deltaCtor.setAccessible(true);

                // duplicate-only delta => newlyAdded stays empty => no append notice branch
                List<MovieResults> duplicateOnly = new ArrayList<>();
                duplicateOnly.add(new MovieResults("1", "Batman One", "2022-01-01", "en",
                                100.0, 8.0, "Action", "Unknown", "N/A", null, null, "Overview 1"));
                SearchRecord duplicateRecord = new SearchRecord("Batman", "movie", duplicateOnly, 19);
                SearchActor.Command duplicateDelta = (SearchActor.Command) deltaCtor.newInstance(duplicateRecord,
                                probe.getRef());
                searchActor.tell(duplicateDelta);
                probe.expectNoMessage(Duration.ofMillis(300));

                // 10 unique new items => appendedCount reaches cap branch
                List<MovieResults> tenNew = new ArrayList<>();
                for (int i = 2; i <= 11; i++) {
                        tenNew.add(new MovieResults(String.valueOf(i), "Movie " + i, "2022-01-0" + ((i % 9) + 1), "en",
                                        90.0 - i, 7.0, "Action", "Unknown", "N/A", null, null, "Overview " + i));
                }
                SearchRecord tenNewRecord = new SearchRecord("Batman", "movie", tenNew, 19);
                SearchActor.Command capDelta = (SearchActor.Command) deltaCtor.newInstance(tenNewRecord,
                                probe.getRef());
                searchActor.tell(capDelta);
                ManagerActor.WrappedSearchAppendNotice notice = probe
                                .expectMessageClass(ManagerActor.WrappedSearchAppendNotice.class);
                assertEquals(10, notice.appendedCount);

                // remaining <= 0 break branch
                List<MovieResults> extraAfterCap = new ArrayList<>();
                extraAfterCap.add(new MovieResults("12", "Movie 12", "2022-01-02", "en",
                                70.0, 6.5, "Action", "Unknown", "N/A", null, null, "Overview 12"));
                SearchRecord extraRecord = new SearchRecord("Batman", "movie", extraAfterCap, 19);
                SearchActor.Command extraDelta = (SearchActor.Command) deltaCtor.newInstance(extraRecord,
                                probe.getRef());
                searchActor.tell(extraDelta);
                probe.expectNoMessage(Duration.ofMillis(300));

                // appendedCount >= 10 branch in onCheckForMoreResults => no tmdb paged search
                Class<?> checkClass = Class.forName("actors.SearchActor$CheckForMoreResults");
                java.lang.reflect.Constructor<?> checkCtor = checkClass.getDeclaredConstructor(String.class,
                                String.class, ActorRef.class);
                checkCtor.setAccessible(true);
                SearchActor.Command check = (SearchActor.Command) checkCtor.newInstance("Batman", "movie",
                                probe.getRef());
                searchActor.tell(check);
                probe.expectNoMessage(Duration.ofMillis(300));
                Mockito.verify(mockTmdbService, Mockito.never()).search(eq("movie"), eq("Batman"), any(Integer.class));
        }

        @Test
        public void testSchedulePolling_TimerTaskExecutes() {
                TmdbServices mockTmdbService = Mockito.mock(TmdbServices.class);
                ObjectMapper mapper = new ObjectMapper();

                ObjectNode initialJson = mapper.createObjectNode();
                initialJson.put("total_results", 1);
                ArrayNode initialResults = mapper.createArrayNode();
                ObjectNode movie = mapper.createObjectNode();
                movie.put("id", "1");
                movie.put("title", "Batman One");
                movie.put("overview", "Overview 1");
                movie.put("release_date", "2022-01-01");
                movie.put("popularity", 100.0);
                movie.put("vote_average", 8.0);
                movie.set("genre_ids", mapper.createArrayNode());
                initialResults.add(movie);
                initialJson.set("results", initialResults);

                ObjectNode emptyPage = mapper.createObjectNode();
                emptyPage.put("total_results", 1);
                emptyPage.set("results", mapper.createArrayNode());

                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman")))
                                .thenReturn(CompletableFuture.completedFuture(initialJson));
                Mockito.when(mockTmdbService.search(eq("movie"), eq("Batman"), eq(1)))
                                .thenReturn(CompletableFuture.completedFuture(emptyPage));
                Mockito.when(mockTmdbService.getGenres(any(), any())).thenReturn("Action");

                ActorRef<SearchActor.Command> searchActor = testKit.spawn(SearchActor.create(mockTmdbService));
                TestProbe<ManagerActor.Command> probe = testKit.createTestProbe();

                searchActor.tell(new SearchActor.PerformSearch("Batman", "movie", probe.getRef()));
                probe.expectMessageClass(ManagerActor.WrappedSearchResponse.class);
                probe.expectMessageClass(ManagerActor.WrappedReadabilityUpdate.class);

                Mockito.verify(mockTmdbService, Mockito.timeout(6000))
                                .search(eq("movie"), eq("Batman"), eq(1));
        }
}
