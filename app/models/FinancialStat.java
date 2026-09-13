package models;

import com.fasterxml.jackson.databind.JsonNode;
import Services.TmdbServices;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Represents the financial statistics of a movie, including budget, revenue, and profit.
 * This model encapsulates the business logic for calculating the Return on Investment (ROI)
 * and categorizing the financial performance into predefined evaluation "buckets".
 * @author Thanugundla Sumith Reddy
 */
public class FinancialStat {

    public final String title;
    public final long budget;
    public final long revenue;
    public final long profit;

    public final String formattedRoi;
    public final String financialRating;

    /**
     * Constructs a new FinancialStat object and automatically calculates the net profit,
     * percentage ROI, and the assigned financial rating bucket.
     * <p>
     * Includes mathematical safeguards to prevent {@code ArithmeticException} (Division by Zero)
     * in cases where the TMDb API returns a movie with a $0 budget.
     *
     * @param title   The official title of the movie.
     * @param budget  The production budget of the movie in USD.
     * @param revenue The worldwide box office gross revenue of the movie in USD.
     * @author Thanugundla Sumith Reddy
     */
    public FinancialStat(String title, long budget, long revenue) {
        this.title = title;
        this.budget = budget;
        this.revenue = revenue;
        this.profit = revenue - budget;

        if (budget > 0) {
            double roi = ((double) this.profit / budget) * 100;
            this.formattedRoi = String.format("%.2f%%", roi);

            if (roi >= 500.0) {
                this.financialRating = "Blockbuster Success";
            } else if (roi >= 200.0) {
                this.financialRating = "High Return";
            } else if (roi >= 0.0) {
                this.financialRating = "Profitable";
            } else {
                this.financialRating = "Financial Loss";
            }
        } else {
            this.formattedRoi = "N/A";
            this.financialRating = "Data Unavailable";
        }
    }

    /**
     * Asynchronously fetches and compiles financial statistics for movies matching a search query.
     * <p>
     * This method orchestrates multiple non-blocking API calls: it first searches for up to 10 matching
     * movies, then concurrently fetches the distinct financial details for each movie. Results that are
     * missing valid budget or revenue data are automatically filtered out.
     *
     * @param query       The search keyword provided by the user.
     * @param tmdbService The initialized service used to communicate with the TMDb API.
     * @return A {@link CompletionStage} resolving to a filtered List of populated {@code FinancialStat} objects.
     * @author Thanugundla Sumith Reddy
     */
    public static CompletionStage<List<FinancialStat>> fetchFinancials(String query, TmdbServices tmdbService) {

        return tmdbService.search("movie", query).thenCompose(searchJson -> {
            List<CompletableFuture<FinancialStat>> futures = new ArrayList<>();
            JsonNode results = searchJson.get("results");

            int count = 0;
            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    if (count >= 10) break;

                    String id = node.get("id").asText();
                    String title = node.has("title") ? node.get("title").asText() : "Unknown";

                    CompletableFuture<FinancialStat> futureStat = tmdbService.getDetails("movie", id)
                            .thenApply(detailsJson -> {
                                long budget = detailsJson.has("budget") ? detailsJson.get("budget").asLong() : 0;
                                long revenue = detailsJson.has("revenue") ? detailsJson.get("revenue").asLong() : 0;

                                return new FinancialStat(title, budget, revenue);
                            }).toCompletableFuture();

                    futures.add(futureStat);
                    count++;
                }
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(f -> {
                                try {
                                    return f.get();
                                } catch (Exception e) {
                                        return null;
                                }
                            })
                            .filter(stat -> stat != null)
                            .filter(stat -> stat.budget > 0 && stat.revenue > 0)
                            .limit(1)
                            .collect(Collectors.toList())
                    );
        });
    }
}