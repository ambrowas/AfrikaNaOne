package utils;



import java.util.HashMap;
import java.util.Map;

public class CountryFlagUtils {

    private static final String STORAGE_BASE_URL = "gs://afrikanaone.firebasestorage.app/flags/";

    // Map of country names to their three-letter ISO abbreviations
    private static final Map<String, String> countryFlagMap = new HashMap<>();

    // Static block to initialize the map with African and selected non-African countries
    static {
        // African countries (three-letter ISO codes)
        countryFlagMap.put("Algeria", "DZA");
        countryFlagMap.put("Angola", "AGO");
        countryFlagMap.put("Benin", "BEN");
        countryFlagMap.put("Botswana", "BWA");
        countryFlagMap.put("Burkina Faso", "BFA");
        countryFlagMap.put("Burundi", "BDI");
        countryFlagMap.put("Cameroon", "CMR");
        countryFlagMap.put("Cape Verde", "CPV");
        countryFlagMap.put("Central African Republic", "CAF");
        countryFlagMap.put("Chad", "TCD");
        countryFlagMap.put("Comoros", "COM");
        countryFlagMap.put("Democratic Republic of Congo", "COD");
        countryFlagMap.put("Republic of the Congo", "COG");
        countryFlagMap.put("Côte d'Ivoire", "CIV");
        countryFlagMap.put("Djibouti", "DJI");
        countryFlagMap.put("Egypt", "EGY");
        countryFlagMap.put("Equatorial Guinea", "GNQ");
        countryFlagMap.put("Eritrea", "ERI");
        countryFlagMap.put("Eswatini", "SWZ");
        countryFlagMap.put("Ethiopia", "ETH");
        countryFlagMap.put("Gabon", "GAB");
        countryFlagMap.put("Gambia", "GMB");
        countryFlagMap.put("Ghana", "GHA");
        countryFlagMap.put("Guinea", "GIN");
        countryFlagMap.put("Guinea-Bissau", "GNB");
        countryFlagMap.put("Kenya", "KEN");
        countryFlagMap.put("Lesotho", "LSO");
        countryFlagMap.put("Liberia", "LBR");
        countryFlagMap.put("Libya", "LBY");
        countryFlagMap.put("Madagascar", "MDG");
        countryFlagMap.put("Malawi", "MWI");
        countryFlagMap.put("Mali", "MLI");
        countryFlagMap.put("Mauritania", "MRT");
        countryFlagMap.put("Mauritius", "MUS");
        countryFlagMap.put("Morocco", "MAR");
        countryFlagMap.put("Mozambique", "MOZ");
        countryFlagMap.put("Namibia", "NAM");
        countryFlagMap.put("Niger", "NER");
        countryFlagMap.put("Nigeria", "NGA");
        countryFlagMap.put("Rwanda", "RWA");
        countryFlagMap.put("São Tomé and Príncipe", "STP");
        countryFlagMap.put("Senegal", "SEN");
        countryFlagMap.put("Seychelles", "SYC");
        countryFlagMap.put("Sierra Leone", "SLE");
        countryFlagMap.put("Somalia", "SOM");
        countryFlagMap.put("South Africa", "ZAF");
        countryFlagMap.put("South Sudan", "SSD");
        countryFlagMap.put("Sudan", "SDN");
        countryFlagMap.put("Tanzania", "TZA");
        countryFlagMap.put("Togo", "TGO");
        countryFlagMap.put("Tunisia", "TUN");
        countryFlagMap.put("Uganda", "UGA");
        countryFlagMap.put("Western Sahara", "ESH");
        countryFlagMap.put("Zambia", "ZMB");
        countryFlagMap.put("Zimbabwe", "ZWE");

        // Non-African countries
        countryFlagMap.put("United States", "USA");
        countryFlagMap.put("Canada", "CAN");
        countryFlagMap.put("Brazil", "BRA");
        countryFlagMap.put("China", "CHN");
        countryFlagMap.put("India", "IND");
        countryFlagMap.put("United Kingdom", "GBR");
        countryFlagMap.put("Germany", "DEU");
        countryFlagMap.put("Spain", "ESP");
        countryFlagMap.put("France", "FRA");
        countryFlagMap.put("Japan", "JPN");
        countryFlagMap.put("Australia", "AUS");
        countryFlagMap.put("European Union", "EUR");
        countryFlagMap.put("Haiti", "HTI");
        countryFlagMap.put("Jamaica", "JAM");
        countryFlagMap.put("Cuba", "CUB");
        countryFlagMap.put("Trinidad and Tobago", "TTO");
        countryFlagMap.put("Barbados", "BRB");
        countryFlagMap.put("Bahamas", "BHS");
        countryFlagMap.put("Guyana", "GUY");
        countryFlagMap.put("Suriname", "SUR");
        countryFlagMap.put("Dominican Republic", "DOM");
        countryFlagMap.put("Antigua and Barbuda", "ATG");
        countryFlagMap.put("Other", "XXX");
    }

    /**
     * Retrieves the ISO 3166-1 alpha-3 abbreviation for a given country name.
     *
     * @param countryName The name of the country.
     * @return The ISO abbreviation, or null if not found.
     */
    public static String getCountryAbbreviation(String countryName) {
        return countryFlagMap.get(countryName);
    }

    /**
     * Retrieves the flag URL for a given country abbreviation.
     *
     * @param abbreviation The ISO abbreviation of the country.
     * @return The URL of the country's flag in Firebase Storage.
     */
    public static String getFlagUrl(String abbreviation) {
        if (abbreviation == null) {
            return null;
        }
        return STORAGE_BASE_URL + abbreviation.toLowerCase() + ".png";
    }

    /**
     * Validates if a country name exists in the map.
     *
     * @param countryName The name of the country.
     * @return True if the country is valid, false otherwise.
     */
    public static boolean isValidCountry(String countryName) {
        return countryFlagMap.containsKey(countryName);
    }
}