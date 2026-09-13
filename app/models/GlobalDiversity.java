package models;

import java.util.List;

/**
 * Model for Global Diversity metrics
 * @author Aman Agnihotri
 */
public class GlobalDiversity {

    public double translationDensity;
    public double localizationIndex;
    public int translationCount;

    /**
 * Constructs a GlobalDiversity object with the computed diversity metrics.
 *
 * @param translationDensity the ratio of available translations relative to the
 *                           total number of languages supported by TMDb.
 *                           A value of 1.0 or above indicates exceptional global coverage.
 * @param localizationIndex  the average ratio of translated overview length to the
 *                           original overview length across all translations.
 *                           A value near 1.0 indicates high quality translations.
 * @param translationCount   the total number of available translations for this item
 * @author Aman Agnihotri
 */
    public GlobalDiversity(double translationDensity, double localizationIndex, int translationCount) {
        this.translationDensity = translationDensity;
        this.localizationIndex = localizationIndex;
        this.translationCount = translationCount;
    }

    /**
     * Calculate Global Diversity metrics
     * @author Aman Agnihotri
     */
    public static GlobalDiversity calculate(List<String> translatedOverviews, String originalOverview,
            int targetLanguages) {

        int count = translatedOverviews.size();

        double translationDensity = (double) count / targetLanguages;

        // double originalLength = originalOverview.length();
        double originalLength = Math.max(originalOverview.length(), 1);

        double localizationIndex = translatedOverviews.stream()
                .mapToDouble(t -> t.length() / originalLength)
                .average()
                .orElse(0.0);

        translationDensity = Math.round(translationDensity * 100.0) / 100.0;
        localizationIndex = Math.round(localizationIndex * 100.0) / 100.0;

        return new GlobalDiversity(translationDensity, localizationIndex, count);
    }
}