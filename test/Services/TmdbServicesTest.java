package Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for TmdbServices.
 * Tests sentiment analysis and genre resolution logic without calling the live
 * TMDb API.
 * 
 * @author Honey Sharma
 * @author Aman Agnihotri
 * @author Varshain Gopichandar Sreedevi
 * @author Thanugundla Sumith Reddy
 */
public class TmdbServicesTest {

    // ========================================================================
    // analyzeSentiment() Tests
    // ========================================================================

    /**
     * Tests that getReviews correctly fetches and returns JSON data from the API.
     * Partition: Successful API response with mock review data.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testGetReviews() throws Exception {
        play.libs.ws.WSClient mockWs = org.mockito.Mockito.mock(play.libs.ws.WSClient.class);
        play.libs.ws.WSRequest mockRequest = org.mockito.Mockito.mock(play.libs.ws.WSRequest.class);
        play.libs.ws.WSResponse mockResponse = org.mockito.Mockito.mock(play.libs.ws.WSResponse.class);
        com.typesafe.config.Config mockConfig = org.mockito.Mockito.mock(com.typesafe.config.Config.class);

        org.mockito.Mockito.when(mockConfig.getString("tmdb.apikey")).thenReturn("test-key");
        org.mockito.Mockito.when(mockWs.url(org.mockito.Mockito.anyString())).thenReturn(mockRequest);
        org.mockito.Mockito
                .when(mockRequest.addQueryParameter(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString()))
                .thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.get())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockResponse));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockJson = mapper.createObjectNode();
        ArrayNode results = mapper.createArrayNode();
        results.add(mapper.createObjectNode().put("author", "Reviewer").put("content", "Great movie!"));
        mockJson.set("results", results);
        org.mockito.Mockito.when(mockResponse.asJson()).thenReturn(mockJson);

        TmdbServices service = new TmdbServices(mockWs, mockConfig);
        JsonNode result = service.getReviews("123", "movie").toCompletableFuture().get();

        assertNotNull(result);
        assertEquals("Reviewer", result.get("results").get(0).get("author").asText());
        assertEquals("Great movie!", result.get("results").get(0).get("content").asText());
    }

    /**
     * Tests that a review with more than 70% happy words returns happy emoticon.
     * Partition: happy words dominate (> 70%).
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentHappy() {
        String review = "great amazing wonderful love perfect excellent fantastic good";
        assertEquals(":-)", TmdbServices.analyzeSentiment(review));
    }

    /**
     * Tests that a review with more than 70% sad words returns sad emoticon.
     * Partition: sad words dominate (> 70%).
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentSad() {
        String review = "terrible awful worst boring hate bad disappointing waste";
        assertEquals(":-(", TmdbServices.analyzeSentiment(review));
    }

    /**
     * Tests that a review with no sentiment words returns neutral emoticon.
     * Partition: no happy or sad words found (total = 0).
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentNoSentimentWords() {
        String review = "the movie was released in 2023 and stars an actor";
        assertEquals(":-|", TmdbServices.analyzeSentiment(review));
    }

    /**
     * Tests that a mixed review with neither happy nor sad exceeding 70% returns
     * neutral.
     * Partition: mixed sentiment, neither side exceeds the threshold (70%).
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentNeutralMixed() {
        String review = "great terrible good bad amazing awful";
        assertEquals(":-|", TmdbServices.analyzeSentiment(review));
    }

    /**
     * Tests sentiment analysis with an empty string input.
     * Partition: boundary — empty review content.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentEmptyString() {
        assertEquals(":-|", TmdbServices.analyzeSentiment(""));
    }

    /**
     * Tests that sentiment analysis is case-insensitive.
     * Partition: uppercase and mixed-case happy words.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentCaseInsensitive() {
        String review = "GREAT AMAZING WONDERFUL LOVE PERFECT EXCELLENT FANTASTIC GOOD";
        assertEquals(":-)", TmdbServices.analyzeSentiment(review));
    }

    /**
     * Tests that a review with exactly one happy word returns happy emoticon.
     * Partition: single happy word, no sad words (100% happy ratio).
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentSingleHappyWord() {
        assertEquals(":-)", TmdbServices.analyzeSentiment("great"));
    }

    /**
     * Tests that a review with exactly one sad word returns sad emoticon.
     * Partition: single sad word, no happy words (100% sad ratio).
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentSingleSadWord() {
        assertEquals(":-(", TmdbServices.analyzeSentiment("terrible"));
    }

    /**
     * Tests sentiment at the exact 70% boundary should return neutral and not
     * happy.
     * Partition: boundary — happy ratio exactly equals 0.7, not exceeding it.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testAnalyzeSentimentAtExactBoundary() {
        // 7 happy, 3 sad = 70% happy — NOT > 0.7, so should be neutral
        String review = "great amazing wonderful love perfect excellent fantastic bad terrible awful";
        assertEquals(":-|", TmdbServices.analyzeSentiment(review));
    }

    // ========================================================================
    // getGenres() Tests
    // ========================================================================

    /**
     * Tests getGenres returns correct genre names for movie category.
     * Partition: valid movie genre IDs present in cache.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testGetGenresMovie() throws Exception {
        // We need a TmdbServices instance — use a subclass to skip API calls
        TmdbServices service = createServiceWithMockGenres();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode genreIds = mapper.createArrayNode();
        genreIds.add(28);
        genreIds.add(12);

        String result = service.getGenres(genreIds, "movie");
        assertEquals("Action, Adventure", result);
    }

    /**
     * Tests getGenres returns correct genre names for TV category.
     * Partition: valid TV genre IDs present in cache.
     * 
     * @author Varshain Gopichandar Sreedevi
     */
    @Test
    public void testGetGenresTv() throws Exception {
        TmdbServices service = createServiceWithMockGenres();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode genreIds = mapper.createArrayNode();
        genreIds.add(10765);

        String result = service.getGenres(genreIds, "tv");
        assertEquals("Sci-Fi & Fantasy", result);
    }

    /**
     * Tests getGenres returns empty string when genre ID not in cache.
     * Partition: genre ID does not exist in the cache map.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testGetGenresUnknownId() throws Exception {
        TmdbServices service = createServiceWithMockGenres();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode genreIds = mapper.createArrayNode();
        genreIds.add(99999); // non-existent ID

        String result = service.getGenres(genreIds, "movie");
        assertEquals("", result);
    }

    /**
     * Tests getGenres returns empty string for empty genre ID array.
     * Partition: boundary — empty array input.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testGetGenresEmptyArray() throws Exception {
        TmdbServices service = createServiceWithMockGenres();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode genreIds = mapper.createArrayNode();

        String result = service.getGenres(genreIds, "movie");
        assertEquals("", result);
    }

    /**
     * Tests getGenres returns comma-separated names for multiple valid IDs.
     * Partition: multiple valid IDs all present in cache.
     * 
     * @author Honey Sharma
     */
    @Test
    public void testGetGenresMultipleIds() throws Exception {
        TmdbServices service = createServiceWithMockGenres();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode genreIds = mapper.createArrayNode();
        genreIds.add(28);
        genreIds.add(12);
        genreIds.add(16);

        String result = service.getGenres(genreIds, "movie");
        assertEquals("Action, Adventure, Animation", result);
    }

    // ========================================================================
    // search(), getDetails(), getGlobalDiversity() Tests
    // ========================================================================

    /**
     * Tests that search correctly fetches and returns JSON search results from the
     * TMDb API.
     * Partition: Successful API response with mock search data (covers all lines
     * and no branches).
     * 
     * @author Aman Agnihotri
     */
    @Test
    public void testSearch() throws Exception {
        play.libs.ws.WSClient mockWs = org.mockito.Mockito.mock(play.libs.ws.WSClient.class);
        play.libs.ws.WSRequest mockRequest = org.mockito.Mockito.mock(play.libs.ws.WSRequest.class);
        play.libs.ws.WSResponse mockResponse = org.mockito.Mockito.mock(play.libs.ws.WSResponse.class);
        com.typesafe.config.Config mockConfig = org.mockito.Mockito.mock(com.typesafe.config.Config.class);

        org.mockito.Mockito.when(mockConfig.getString("tmdb.apikey")).thenReturn("test-key");
        org.mockito.Mockito.when(mockWs.url(org.mockito.Mockito.anyString())).thenReturn(mockRequest);
        org.mockito.Mockito
                .when(mockRequest.addQueryParameter(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString()))
                .thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.get())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockResponse));

        ObjectMapper mapper = new ObjectMapper();

        // Genre JSONs for constructor (loadMovieGenres + loadTvGenres)
        ObjectNode movieGenresJson = mapper.createObjectNode();
        ArrayNode movieGenres = mapper.createArrayNode();
        movieGenres.add(mapper.createObjectNode().put("id", 28).put("name", "Action"));
        movieGenresJson.set("genres", movieGenres);

        ObjectNode tvGenresJson = mapper.createObjectNode();
        ArrayNode tvGenres = mapper.createArrayNode();
        tvGenres.add(mapper.createObjectNode().put("id", 10765).put("name", "Sci-Fi & Fantasy"));
        tvGenresJson.set("genres", tvGenres);

        // Search results JSON
        ObjectNode searchJson = mapper.createObjectNode();
        ArrayNode results = mapper.createArrayNode();
        results.add(mapper.createObjectNode().put("id", 123).put("title", "Test Movie"));
        searchJson.set("results", results);
        searchJson.put("total_results", 10);

        org.mockito.Mockito.when(mockResponse.asJson())
                .thenReturn(movieGenresJson)
                .thenReturn(tvGenresJson)
                .thenReturn(searchJson);

        TmdbServices service = new TmdbServices(mockWs, mockConfig);
        JsonNode result = service.search("movie", "test query").toCompletableFuture().get();

        assertNotNull(result);
        assertEquals(10, result.get("total_results").asInt());
        assertEquals("Test Movie", result.get("results").get(0).get("title").asText());
    }

    @Test
    public void testSearchWithPageParameter() throws Exception {
        play.libs.ws.WSClient mockWs = org.mockito.Mockito.mock(play.libs.ws.WSClient.class);
        play.libs.ws.WSRequest mockRequest = org.mockito.Mockito.mock(play.libs.ws.WSRequest.class);
        play.libs.ws.WSResponse mockResponse = org.mockito.Mockito.mock(play.libs.ws.WSResponse.class);
        com.typesafe.config.Config mockConfig = org.mockito.Mockito.mock(com.typesafe.config.Config.class);

        org.mockito.Mockito.when(mockConfig.getString("tmdb.apikey")).thenReturn("test-key");
        org.mockito.Mockito.when(mockWs.url(org.mockito.Mockito.anyString())).thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.addQueryParameter(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString()))
                .thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.get())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockResponse));
        org.mockito.Mockito.when(mockResponse.asJson()).thenReturn(new ObjectMapper().createObjectNode());

        TmdbServices service = new TmdbServices(mockWs, mockConfig);
        service.search("movie", "iron man", 2).toCompletableFuture().get();

        org.mockito.Mockito.verify(mockRequest).addQueryParameter("page", "2");
    }

    /**
     * Tests getDetails for both non-person and person categories.
     * Covers every line and the if-branch for "person" (append_to_response).
     * 
     * @author Aman Agnihotri
     */
    @Test
    public void testGetDetails() throws Exception {
        play.libs.ws.WSClient mockWs = org.mockito.Mockito.mock(play.libs.ws.WSClient.class);
        play.libs.ws.WSRequest mockRequest = org.mockito.Mockito.mock(play.libs.ws.WSRequest.class);
        play.libs.ws.WSResponse mockResponse = org.mockito.Mockito.mock(play.libs.ws.WSResponse.class);
        com.typesafe.config.Config mockConfig = org.mockito.Mockito.mock(com.typesafe.config.Config.class);

        org.mockito.Mockito.when(mockConfig.getString("tmdb.apikey")).thenReturn("test-key");
        org.mockito.Mockito.when(mockWs.url(org.mockito.Mockito.anyString())).thenReturn(mockRequest);
        org.mockito.Mockito
                .when(mockRequest.addQueryParameter(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString()))
                .thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.get())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockResponse));

        ObjectMapper mapper = new ObjectMapper();

        // Genre JSONs for constructor (loadMovieGenres + loadTvGenres)
        ObjectNode movieGenresJson = mapper.createObjectNode();
        ArrayNode movieGenres = mapper.createArrayNode();
        movieGenres.add(mapper.createObjectNode().put("id", 28).put("name", "Action"));
        movieGenresJson.set("genres", movieGenres);

        ObjectNode tvGenresJson = mapper.createObjectNode();
        ArrayNode tvGenres = mapper.createArrayNode();
        tvGenres.add(mapper.createObjectNode().put("id", 10765).put("name", "Sci-Fi & Fantasy"));
        tvGenresJson.set("genres", tvGenres);

        // Details JSON (same for both calls)
        ObjectNode detailsJson = mapper.createObjectNode();
        detailsJson.put("id", 123);
        detailsJson.put("title", "Test Item");
        detailsJson.put("overview", "Test overview for details");

        org.mockito.Mockito.when(mockResponse.asJson())
                .thenReturn(movieGenresJson)
                .thenReturn(tvGenresJson)
                .thenReturn(detailsJson) // non-person call
                .thenReturn(detailsJson); // person call

        TmdbServices service = new TmdbServices(mockWs, mockConfig);

        // Non-person category (false branch of if)
        JsonNode resultNonPerson = service.getDetails("movie", "123").toCompletableFuture().get();
        assertNotNull(resultNonPerson);
        assertEquals("Test Item", resultNonPerson.get("title").asText());

        // Person category (true branch of if - append_to_response)
        JsonNode resultPerson = service.getDetails("person", "456").toCompletableFuture().get();
        assertNotNull(resultPerson);
        assertEquals("Test Item", resultPerson.get("title").asText());
    }

    /**
     * Tests getGlobalDiversity by mocking both API calls and the
     * translation-processing loop.
     * Covers every line and the if (data.has("overview")) branch (true + false
     * paths).
     * 
     * @author Aman Agnihotri
     */
    @Test
    public void testGetGlobalDiversity() throws Exception {
        play.libs.ws.WSClient mockWs = org.mockito.Mockito.mock(play.libs.ws.WSClient.class);
        play.libs.ws.WSRequest mockRequest = org.mockito.Mockito.mock(play.libs.ws.WSRequest.class);
        play.libs.ws.WSResponse mockResponse = org.mockito.Mockito.mock(play.libs.ws.WSResponse.class);
        com.typesafe.config.Config mockConfig = org.mockito.Mockito.mock(com.typesafe.config.Config.class);

        org.mockito.Mockito.when(mockConfig.getString("tmdb.apikey")).thenReturn("test-key");
        org.mockito.Mockito.when(mockWs.url(org.mockito.Mockito.anyString())).thenReturn(mockRequest);
        org.mockito.Mockito
                .when(mockRequest.addQueryParameter(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString()))
                .thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.get())
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockResponse));

        ObjectMapper mapper = new ObjectMapper();

        // Genre JSONs for constructor (loadMovieGenres + loadTvGenres)
        ObjectNode movieGenresJson = mapper.createObjectNode();
        ArrayNode movieGenres = mapper.createArrayNode();
        movieGenres.add(mapper.createObjectNode().put("id", 28).put("name", "Action"));
        movieGenresJson.set("genres", movieGenres);

        ObjectNode tvGenresJson = mapper.createObjectNode();
        ArrayNode tvGenres = mapper.createArrayNode();
        tvGenres.add(mapper.createObjectNode().put("id", 10765).put("name", "Sci-Fi & Fantasy"));
        tvGenresJson.set("genres", tvGenres);

        // Movie JSON with translations (covers both sides of if (data.has("overview")))
        ObjectNode movieJson = mapper.createObjectNode();
        movieJson.put("overview", "Original English overview.");

        ObjectNode translationsWrapper = mapper.createObjectNode();
        ArrayNode translationsList = mapper.createArrayNode();

        // Translation entry with overview → if branch true
        ObjectNode transWithOverview = mapper.createObjectNode();
        ObjectNode dataWith = mapper.createObjectNode();
        dataWith.put("overview", "Aperçu français");
        transWithOverview.set("data", dataWith);
        translationsList.add(transWithOverview);

        // Translation entry without overview → if branch false
        ObjectNode transWithoutOverview = mapper.createObjectNode();
        ObjectNode dataWithout = mapper.createObjectNode();
        transWithoutOverview.set("data", dataWithout);
        translationsList.add(transWithoutOverview);

        translationsWrapper.set("translations", translationsList);
        movieJson.set("translations", translationsWrapper);

        // Languages JSON (array of 3 supported languages)
        ArrayNode languagesJson = mapper.createArrayNode();
        languagesJson.add(mapper.createObjectNode().put("iso_639_1", "en"));
        languagesJson.add(mapper.createObjectNode().put("iso_639_1", "fr"));
        languagesJson.add(mapper.createObjectNode().put("iso_639_1", "es"));

        org.mockito.Mockito.when(mockResponse.asJson())
                .thenReturn(movieGenresJson)
                .thenReturn(tvGenresJson)
                .thenReturn(movieJson) // movieFuture
                .thenReturn(languagesJson); // languagesFuture

        TmdbServices service = new TmdbServices(mockWs, mockConfig);

        models.GlobalDiversity result = service.getGlobalDiversity(123).toCompletableFuture().get();

        assertNotNull(result);
    }

    // ========================================================================
    // Helper — creates a TmdbServices with pre-populated genre caches
    // without making any real API calls
    // ========================================================================

    /**
     * Creates a TmdbServices instance with pre-populated genre caches for testing.
     * Bypasses API calls by directly injecting known genre mappings via reflection.
     * 
     * @return TmdbServices instance with mock genre data
     * @throws Exception if reflection access fails
     * @author Thanugundla Sumith Reddy
     */
    private TmdbServices createServiceWithMockGenres() throws Exception {
        // Use Mockito to create a partial mock — skip real WS calls
        play.libs.ws.WSClient mockWs = org.mockito.Mockito.mock(play.libs.ws.WSClient.class);
        com.typesafe.config.Config mockConfig = org.mockito.Mockito.mock(com.typesafe.config.Config.class);
        org.mockito.Mockito.when(mockConfig.getString("tmdb.apikey")).thenReturn("test-key");

        // Mock the WS calls made in loadMovieGenres() and loadTvGenres()
        play.libs.ws.WSRequest mockRequest = org.mockito.Mockito.mock(play.libs.ws.WSRequest.class);
        play.libs.ws.WSResponse mockResponse = org.mockito.Mockito.mock(play.libs.ws.WSResponse.class);

        ObjectMapper mapper = new ObjectMapper();

        // Build mock movie genres JSON
        ObjectNode movieGenresJson = mapper.createObjectNode();
        ArrayNode movieGenres = mapper.createArrayNode();
        addGenre(mapper, movieGenres, 28, "Action");
        addGenre(mapper, movieGenres, 12, "Adventure");
        addGenre(mapper, movieGenres, 16, "Animation");
        movieGenresJson.set("genres", movieGenres);

        // Build mock TV genres JSON
        ObjectNode tvGenresJson = mapper.createObjectNode();
        ArrayNode tvGenres = mapper.createArrayNode();
        addGenre(mapper, tvGenres, 10765, "Sci-Fi & Fantasy");
        tvGenresJson.set("genres", tvGenres);

        org.mockito.Mockito.when(mockWs.url(org.mockito.Mockito.anyString())).thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.addQueryParameter(
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString())).thenReturn(mockRequest);
        org.mockito.Mockito.when(mockRequest.get()).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(mockResponse));

        // Return movie or TV genres based on URL — first call = movie, second = TV
        org.mockito.Mockito.when(mockResponse.asJson())
                .thenReturn(movieGenresJson)
                .thenReturn(tvGenresJson);

        return new TmdbServices(mockWs, mockConfig);
    }

    /**
     * Helper to add a genre node to a JSON array.
     * 
     * @param mapper the ObjectMapper used to create nodes
     * @param array  the ArrayNode to add the genre to
     * @param id     the genre ID
     * @param name   the genre name
     * @author Varshain Gopichandar Sreedevi
     */
    private void addGenre(ObjectMapper mapper, ArrayNode array, int id, String name) {
        ObjectNode genre = mapper.createObjectNode();
        genre.put("id", id);
        genre.put("name", name);
        array.add(genre);
    }
}
