package fr.ribesg.bukkit.ncore;

import fr.ribesg.bukkit.ncore.nodes.NPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Represents a config file
 *
 * @param <T> The Node type
 *
 * @author Ribesg
 */
public abstract class AbstractConfig<T extends NPlugin> {

    /** The Charset used for reading/writing files */
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    /** The Plugin linked to this config */
    protected final T plugin;

    /**
     * Constructor
     *
     * @param instance Linked plugin instance
     */
    public AbstractConfig(final T instance) {
        plugin = instance;
    }

    /**
     * Default fileName to config.yml in call to {@link #loadConfig(String)}
     *
     * @throws IOException If there is an error reading / writing file
     */
    public void loadConfig() throws IOException {
        loadConfig("config.yml");
    }

    /**
     * Load the config containing messages
     * Creates a new config if it does not exists
     * Fix the config after parsing
     *
     * @param fileName The name of the file to load
     *
     * @throws IOException If there is an error reading / writing file
     */
    public void loadConfig(final String fileName) throws IOException {
        final Path path = Paths.get(plugin.getDataFolder().toPath().toAbsolutePath().toString() + File.separator + fileName);
        if (!Files.exists(path)) {
            Files.createFile(path);
            writeConfig(path);
        } else {
            final YamlConfiguration config = new YamlConfiguration();
            try (BufferedReader reader = Files.newBufferedReader(path, CHARSET)) {
                final StringBuilder s = new StringBuilder();
                while (reader.ready()) {
                    s.append(reader.readLine()).append('\n');
                }
                config.loadFromString(s.toString());
            } catch (final Exception e) {
                e.printStackTrace();
            }

            setValues(config);

            // Rewrite the config to "clean" it
            writeConfig(path);
        }
    }

    public void writeConfig() throws IOException {
        writeConfig("config.yml");
    }

    public void writeConfig(final String fileName) throws IOException {
        final Path path = Paths.get(plugin.getDataFolder().toPath().toAbsolutePath().toString() + File.separator + fileName);
        writeConfig(path);
    }

    private void writeConfig(final Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, CHARSET, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            writer.write(getConfigString());
        }
    }

    protected void wrongValue(final String fileName, final String key, final Object incorrectValue, final Object valueSet) {
        final StringBuilder message1 = new StringBuilder();
        message1.append("Incorrect value '").append(incorrectValue.toString());
        message1.append("' found in config file ").append(fileName);
        message1.append(" for key '").append(key).append("'");

        final StringBuilder message2 = new StringBuilder();
        message2.append("The value of config key '").append(key);
        message2.append("' as been reset to '").append(valueSet.toString());
        message2.append("' in file ").append(fileName);

        plugin.getLogger().warning(message1.toString());
        plugin.getLogger().info(message2.toString());
    }

    public static boolean match(final long value, final long min, final long max) {
        return min <= value && value <= max;
    }

    public static boolean match(final double value, final double min, final double max) {
        return min <= value && value <= max;
    }

    /**
     * Set the values in the config to there current values
     *
     * @param config The config where to set values
     */
    protected abstract void setValues(final YamlConfiguration config);

    /** @return the String to be written to file */
    protected abstract String getConfigString();
}
