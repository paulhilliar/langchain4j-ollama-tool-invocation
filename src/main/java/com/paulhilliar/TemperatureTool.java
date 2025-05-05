package com.paulhilliar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TemperatureTool {

    // Mapping of country codes to capital cities (simplified for this example)
    private static final Map<String, String> COUNTRY_TO_CAPITAL = new HashMap<>();

    static {
        // Populate the maps for a few countries
        COUNTRY_TO_CAPITAL.put("GBR", "London");
        COUNTRY_TO_CAPITAL.put("FRA", "Paris");
        COUNTRY_TO_CAPITAL.put("USA", "New York"); // Using New York as a major US city with a distinct timezone, though US has multiple. A real tool would be more complex.
        COUNTRY_TO_CAPITAL.put("JPN", "Tokyo");
        COUNTRY_TO_CAPITAL.put("AUS", "Sydney"); // Using Sydney for timezone example, capital is Canberra
        COUNTRY_TO_CAPITAL.put("IND", "New Delhi");
    }

    /**
     * Retrieves the current weather conditions.
     *
     * This demonstrates how a LLM can interpret a JSON response to extract useful information.
     *
     * If you ask the chatbot "What temperature is it in France?" then you may get a response like
     * "The temperature in Paris, France is 12.2°C with a clear sky."
     *
     * @param countryCode The 3-letter country code (e.g., GBR, USA, FRA).
     * @return A JSON string representing the weather
     */
    @Tool("""
          Gets the current weather conditions in the capital city of a given country.
          Provide the country using its 3-letter code (e.g., GBR, USA, FRA).
          Supported codes include GBR, FRA, USA, JPN, AUS, IND.
          """)
    public String getCurrentWeatherInCapital(String countryCode) throws Exception {
        log.info("getCurrentWeatherInCapital invoked with country code: {}", countryCode);
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

        val weatherData = getCurrentWeather(capital);
        String responseJson = objectMapper.writeValueAsString(weatherData);
        log.info("For {} in {} we got weather: {}", capital, countryCode, responseJson);
        return responseJson;
    }

    private static final String API_KEY = "8273fab7664b45109491158e636ac0e3";
    private static final String BASE_URL = "https://api.weatherbit.io/v2.0/current";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gets current weather conditions for a given city.
     *
     * @param city The name of the city (e.g., "Paris", "London", "New York").
     * @return A WeatherData object containing the weather details (the first one from the list),
     *         or null if the API call fails, no data is returned, or parsing fails.
     * @throws IOException If an I/O error occurs when sending the request.
     * @throws InterruptedException If the operation is interrupted.
     * @throws IllegalStateException if the API key is not set.
     */
    public WeatherData getCurrentWeather(String city) throws IOException, InterruptedException {
        if (city == null || city.trim().isEmpty()) {
            System.err.println("City name cannot be empty.");
            return null;
        }

        // URL-encode the city name to handle spaces and special characters
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());

        // Build the full API URL
        String apiUrl = String.format("%s?city=%s&key=%s", BASE_URL, encodedCity, API_KEY);

        // Create the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Accept", "application/json") // Request JSON response
            .GET() // Explicitly set GET method (default for no body, but good practice)
            .build();

        log.info("Calling API: {}", apiUrl); // For debugging

        // Send the request and get the response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check the HTTP status code
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode != 200) {
            log.error("API call failed with status code: " + statusCode);
            log.error("Response Body: " + responseBody);
            return null; // Indicate failure
        }

        // Parse the JSON response into our Records
        try {
            // Jackson automatically deserializes the JSON into our WeatherbitResponse record,
            // which contains a list of WeatherData records, each containing a WeatherDescription record.
            WeatherbitResponse weatherResponse = objectMapper.readValue(responseBody, WeatherbitResponse.class);

            // Weatherbit returns a list of data points, usually just one for a city query
            List<WeatherData> data = weatherResponse.data(); // Use record accessor method .data()

            if (data != null && !data.isEmpty()) {
                return data.get(0); // Return the first (and usually only) data point
            } else {
                log.error("API returned success (200), but no weather data was found for city: " + city);
                log.error("Response Body: " + responseBody);
                return null;
            }

        } catch (Exception e) {
            log.error("Error parsing JSON response:", e);
            log.error("Response Body: " + responseBody); // Print raw body for debugging
            return null; // Indicate parsing failure
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WeatherDescription(
        String description // e.g., "Clear Sky"
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WeatherData(
        double temp,           // Temperature in Celsius (default)
        @JsonProperty("city_name") String cityName, // Use @JsonProperty if JSON field name differs or for clarity
        String datetime,       // e.g., "2023-10-27:11"
        WeatherDescription weather, // Nested weather details
        @JsonProperty("app_temp") double appTemp  // Apparent temperature
        // Add more fields from the API response here (e.g., wind_spd, pressure, etc.)
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WeatherbitResponse(
        List<WeatherData> data // The API returns a list under the "data" key
    ) {}

}
