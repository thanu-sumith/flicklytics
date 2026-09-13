package models;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the {@link GlobalDiversity} model.
 * Verifies the calculation logic for translation metrics, including density,
 * localization indices, and edge case handling for empty inputs.
 * * @author Aman Agnihotri
 */
public class GlobalDiversityTest {

    /**
     * Verifies that the constructor correctly initializes the model fields.
     */
    @Test
    public void testConstructor() {
        GlobalDiversity gd = new GlobalDiversity(0.5, 0.8, 3);
        assertEquals(0.5, gd.translationDensity, 0.001);
        assertEquals(0.8, gd.localizationIndex, 0.001);
        assertEquals(3, gd.translationCount);
    }

    /**
     * Tests the calculation logic using a standard list of translations.
     * Ensures that count, density, and localization indices are computed accurately
     * under normal operating conditions.
     *  * * @author Aman Agnihotri
     */
    @Test
    public void testCalculateNormal() {
        List<String> translations = Arrays.asList("Hello world", "Bonjour monde", "Hola mundo");
        GlobalDiversity result = GlobalDiversity.calculate(translations, "Hello world", 10);

        assertEquals(3, result.translationCount);
        assertEquals(0.3, result.translationDensity, 0.01);
        // localizationIndex should be around 1.0 since lengths are similar
        assertEquals(1.0, result.localizationIndex, 0.1);
    }

    /**
     * Verifies that the calculation handles an empty translations list gracefully.
     * This specifically tests the fallback branch where default values (0.0) are
     * returned.
     *  * * @author Aman Agnihotri
     */
    @Test
    public void testCalculateEmptyTranslations() {
        List<String> translations = new ArrayList<>();
        GlobalDiversity result = GlobalDiversity.calculate(translations, "Hello world", 10);

        assertEquals(0, result.translationCount);
        assertEquals(0.0, result.translationDensity, 0.001);
        assertEquals(0.0, result.localizationIndex, 0.001);
    }

    /**
     * Tests the edge case where the original overview text is empty.
     * Ensures the calculation avoids division by zero by utilizing a minimum
     * length of 1 in the calculation logic.
     *  * * @author Aman Agnihotri
     */
    @Test
    public void testCalculateEmptyOriginalOverview() {
        List<String> translations = Arrays.asList("Hello");
        GlobalDiversity result = GlobalDiversity.calculate(translations, "", 5);

        assertEquals(1, result.translationCount);
        // originalLength becomes 1 due to Math.max logic
        assertEquals(5.0, result.localizationIndex, 0.01);
    }

    /**
     * Verifies that the translation density is correctly rounded to two decimal
     * places.
     * For example, 2/3 (0.666...) should be represented as 0.67.
     *  * * @author Aman Agnihotri
     */
    @Test
    public void testCalculateRounding() {
        List<String> translations = Arrays.asList("Hi", "Hola");
        GlobalDiversity result = GlobalDiversity.calculate(translations, "Hello", 3);

        assertEquals(0.67, result.translationDensity, 0.01);
    }

    /**
     * Verifies that the model correctly calculates metrics when only
     * a single translation is provided.
     *  * * @author Aman Agnihotri
     */
    @Test
    public void testCalculateSingleTranslation() {
        List<String> translations = Arrays.asList("Ciao mondo");
        GlobalDiversity result = GlobalDiversity.calculate(translations, "Hello world", 5);

        assertEquals(1, result.translationCount);
        assertEquals(0.2, result.translationDensity, 0.01);
    }
}