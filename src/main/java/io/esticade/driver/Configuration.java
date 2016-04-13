package io.esticade.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import static java.util.Arrays.asList;

class Configuration {
    public String amqpUrl;
    public boolean engraved;
    public String exchange;

    private static Configuration config;

    private Configuration(){
        setDefaults();
        loadConfigFromFiles();
    }

    private void loadConfigFromFiles() {
        try {
            String configFile = asList(
                getConfigFromEnv(),
                getConfigFromCwd(),
                getConfigFromUserHome(),
                getGlobalConfig()
            ).stream()
                .filter((item) -> item != null)
                .filter((item) -> new File(item).exists())
                .findFirst()
                .get();

            parseConfig(configFile);
        } catch (NoSuchElementException e) {
            // No configuration found
        }
    }

    private void parseConfig(String configFile) {
        ObjectMapper om = new ObjectMapper();
        try {
            JsonNode json = om.readTree(new File(configFile));
            amqpUrl = json.hasNonNull("connectionURL")?json.get("connectionURL").asText():amqpUrl;
            exchange = json.hasNonNull("exchange")?json.get("exchange").asText():exchange;
            engraved = json.hasNonNull("engraved")?json.get("engraved").asBoolean():engraved;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getGlobalConfig() {
        return "/etc/esticade/esticaderc";
    }

    private String getConfigFromUserHome() {
        return Paths.get(System.getProperty("user.home")).resolve(".esticaderc").toString();
    }

    private String getConfigFromCwd() {
        Path path = Paths.get("").toAbsolutePath();
        do {
            File file = path.resolve("esticade.json").toFile();
            if (file.exists()) {
                return file.getAbsolutePath();
            }
            path = path.getParent();
        } while (path != null);

        return null;
    }

    private String getConfigFromEnv() {
        String configFile = System.getenv("ESTICADERC");
        return configFile;
    }

    private void setDefaults() {
        amqpUrl = "amqp://guest:guest@localhost/";
        engraved = false;
        exchange = "events";
    }

    public static Configuration getConfig() {
        if(config != null)
            return config;

        config = new Configuration();
        return config;
    }
}
