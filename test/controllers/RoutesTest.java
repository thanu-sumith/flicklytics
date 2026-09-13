// package controllers;

// import org.junit.Test;
// import play.Application;
// import play.inject.guice.GuiceApplicationBuilder;
// import play.mvc.Http;
// import play.mvc.Result;
// import play.test.WithApplication;

// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertNotEquals;
// import static org.junit.Assert.assertNotNull;
// import static play.mvc.Http.Status.NOT_FOUND;
// import static play.test.Helpers.GET;
// import static play.test.Helpers.route;

// /**
//  * Integration tests to verify that the conf/routes file is correctly mapped.
//  * Excludes incomplete features (Part E).
//  */
// public class RoutesTest extends WithApplication {

//     @Override
//     protected Application provideApplication() {
//         // Builds a lightweight fake application just to test the routing engine
//         return new GuiceApplicationBuilder().build();
//     }

//     @Test
//     public void testValidRoutes() {
//         // 1. Test Index/Home Route
//         Http.RequestBuilder indexRequest = new Http.RequestBuilder().method(GET).uri("/");
//         Result indexResult = route(app, indexRequest);
//         assertNotNull("Index route should exist", indexResult);
//         assertNotEquals(NOT_FOUND, indexResult.status());

//         // 2. Test Core Search Route
//         Http.RequestBuilder searchRequest = new Http.RequestBuilder().method(GET)
//                 .uri("/search?query=Inception&category=movie");
//         Result searchResult = route(app, searchRequest);
//         assertNotNull("Search route should exist", searchResult);
//         assertNotEquals(NOT_FOUND, searchResult.status());

//         // 3. Test Item Details Route (Part A)
//         Http.RequestBuilder detailsRequest = new Http.RequestBuilder().method(GET).uri("/itemDetails/movie/12345");
//         Result detailsResult = route(app, detailsRequest);
//         assertNotNull("Item Details route should exist", detailsResult);
//         assertNotEquals(NOT_FOUND, detailsResult.status());

//         // 4. Test Global Diversity Route (Part B)
//         Http.RequestBuilder diversityRequest = new Http.RequestBuilder().method(GET).uri("/globaldiversity/27205");
//         Result diversityResult = route(app, diversityRequest);
//         assertNotNull("Diversity route should exist", diversityResult);
//         assertNotEquals(NOT_FOUND, diversityResult.status());

//         // 5. Test Financial Performance Route (Part C)
//         Http.RequestBuilder financeRequest = new Http.RequestBuilder().method(GET).uri("/finance/Avatar");
//         Result financeResult = route(app, financeRequest);
//         assertNotNull("Financial Performance route should exist", financeResult);
//         assertNotEquals(NOT_FOUND, financeResult.status());

//         // 6. Test Person Stats Route (Part D)
//         Http.RequestBuilder personRequest = new Http.RequestBuilder().method(GET).uri("/person/123/stats");
//         Result personResult = route(app, personRequest);
//         assertNotNull("Person Stats route should exist", personResult);
//         assertNotEquals(NOT_FOUND, personResult.status());
//     }

//     @Test
//     public void testBadRouteReturns404() {
//         // Verify that the router correctly catches URLs that don't exist
//         Http.RequestBuilder badRequest = new Http.RequestBuilder()
//                 .method(GET)
//                 .uri("/this-is-a-fake-url-that-should-fail");

//         Result badResult = route(app, badRequest);

//         assertNotNull(badResult);
//         assertEquals(NOT_FOUND, badResult.status());
//     }
// }