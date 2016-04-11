package io.esticade.driver;


import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
        getConfigFromEnv();
    }

    private void loadConfigFromFiles() {
        List<String> configFiles = asList(
            getConfigFromEnv(),
            getConfigFromCwd(),
            getConfigFromUserHome(),
            getGlobalConfig()
        );

        String configFile = null;
        try {
            configFile = configFiles.stream()
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
        try {
            JsonObject json = (JsonObject) Json.createReader(new FileInputStream(configFile)).read();
            amqpUrl = json.getString("connectionURL", amqpUrl);
            exchange = json.getString("exchange", exchange);
            engraved = json.getBoolean("engraved", engraved);
        } catch (FileNotFoundException e) {
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
