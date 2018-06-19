package cz.xtf.core.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Loads properties stored in several different ways. Possible options, with top to down overriding are: </br>
 *
 * <ul>
 *	<li><i>global-test.properties</i> on project root path</li>
 *	<li><i>test.properties</i> on project root path - meant to be user specific, unshared</li>
 *	<li><i>environment variables</i> in System.getEnv()</li>
 *	<li><i>system properties</i> in System.getProperties()</li>
 * </ul>
 */
@Slf4j
public final class XTFConfig {
	// Replace with common method for finding once available in core
	private static final Path testPropertiesPath;
	private static final Path globalPropertiesPath;

	private static final Properties properties = new Properties();

	// Pre-loading
	static {
		testPropertiesPath = XTFConfig.getProjectRoot().resolve("test.properties");
		globalPropertiesPath = XTFConfig.getProjectRoot().resolve("global-test.properties");

		properties.putAll(XTFConfig.getPropertiesFromPath(globalPropertiesPath));
		properties.putAll(XTFConfig.getPropertiesFromPath(testPropertiesPath));
		properties.putAll(System.getenv().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().replaceAll("_", ".").toLowerCase(), Map.Entry::getValue)));
		properties.putAll(System.getProperties());
	}

	public static String get(String property) {
		return properties.getProperty(property);
	}

	public static String get(String property, String fallbackValue) {
		return properties.getProperty(property, fallbackValue);
	}

	private static Path getProjectRoot() {
		Path dir = Paths.get("").toAbsolutePath();
		while(dir.getParent().resolve("pom.xml").toFile().exists()) dir = dir.getParent();
		return dir;
	}

	private static Properties getPropertiesFromPath(Path path) {
		Properties properties = new Properties();

		if(Files.isReadable(path)) {
			try (InputStream is = Files.newInputStream(testPropertiesPath)) {
				properties.load(is);
			} catch (final IOException ex) {
				log.warn("Unable to read properties from '{}'", path, ex);
			}
		}

		return properties;
	}
}