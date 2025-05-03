package com.paulhilliar;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TimeTool {

    // Mapping of country codes to capital cities (simplified for this example)
    private static final Map<String, String> COUNTRY_TO_CAPITAL = new HashMap<>();
    // Mapping of capital cities to Timezone IDs
    private static final Map<String, String> CAPITAL_TO_TIMEZONE_ID = new HashMap<>();

    static {
        // Populate the maps for a few countries
        COUNTRY_TO_CAPITAL.put("GBR", "London");
        COUNTRY_TO_CAPITAL.put("FRA", "Paris");
        COUNTRY_TO_CAPITAL.put("USA", "New York"); // Using New York as a major US city with a distinct timezone, though US has multiple. A real tool would be more complex.
        COUNTRY_TO_CAPITAL.put("JPN", "Tokyo");
        COUNTRY_TO_CAPITAL.put("AUS", "Sydney"); // Using Sydney for timezone example, capital is Canberra
        COUNTRY_TO_CAPITAL.put("IND", "New Delhi");

        // Populate Timezone IDs based on the cities above
        CAPITAL_TO_TIMEZONE_ID.put("London", "Europe/London");
        CAPITAL_TO_TIMEZONE_ID.put("Paris", "Europe/Paris");
        CAPITAL_TO_TIMEZONE_ID.put("New York", "America/New_York");
        CAPITAL_TO_TIMEZONE_ID.put("Tokyo", "Asia/Tokyo");
        CAPITAL_TO_TIMEZONE_ID.put("Sydney", "Australia/Sydney");
        CAPITAL_TO_TIMEZONE_ID.put("New Delhi", "Asia/Kolkata"); // IST

        // Add more mappings as needed for your Ops chatbot
    }

    /**
     * Retrieves the current time in the capital city of a given country code.
     *
     * @param countryCode The 3-letter country code (e.g., GBR, USA, FRA).
     * @return A string representing the current date and time in the capital's timezone,
     *         or an error message if the country code is not supported.
     */
    @Tool("""
          Gets the current date and time in the capital city of a given country. 
          Provide the country using its 3-letter code (e.g., GBR, USA, FRA). 
          Supported codes include GBR, FRA, USA, JPN, AUS, IND.
          """)
    public String getCurrentTimeInCapital(String countryCode) {
        log.info("getCurrentTimeInCapital invoked with country code: {}", countryCode);
        if (countryCode == null || countryCode.trim().isEmpty()) {
            return "Error: Country code cannot be empty.";
        }

        // Capitalize input to match map keys
        String upperCountryCode = countryCode.trim().toUpperCase();

        // 1. Find the capital city
        String capital = COUNTRY_TO_CAPITAL.get(upperCountryCode);

        if (capital == null) {
            return "Error: Country code '" + countryCode + "' not supported or found.";
        }

        // 2. Find the timezone ID for the capital
        String timezoneId = CAPITAL_TO_TIMEZONE_ID.get(capital);

         if (timezoneId == null) {
            // This should ideally not happen if maps are consistent, but good defensive check
            return "Error: Timezone information not available for capital '" + capital + "'.";
        }


        try {
            // 3. Get the current time in that timezone
            ZoneId zone = ZoneId.of(timezoneId);
            ZonedDateTime now = ZonedDateTime.now(zone);

            // 4. Format the time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzzz"); // e.g., 2023-10-27 10:30:00 British Summer Time
            String formattedTime = now.format(formatter);

            String resultMsg = "The current time in " + capital + " (" + upperCountryCode + ") is: " + formattedTime;
            log.info("Result message: {}", resultMsg);
            return resultMsg;

        } catch (Exception e) {
             System.err.println("Error getting time for timezone " + timezoneId + ": " + e.getMessage());
             return "Error retrieving time for " + capital + ".";
        }
    }
}
