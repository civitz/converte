package converte.utils;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class Logging {
	static final String LOG_FILE_NAME = "converte.log";

	public static void setupLogger() {
		try {
			String baseDir = System.getProperty("user.dir");
			Layout layout = new PatternLayout("%d [%t] %p %c: %m%n");
			String filename = Paths.get(baseDir, LOG_FILE_NAME).toAbsolutePath().toString();
			RollingFileAppender appender = new RollingFileAppender(layout, filename, true);
			appender.setMaxBackupIndex(2);
			appender.setMaxFileSize("10MB");
			BasicConfigurator.configure(appender);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
