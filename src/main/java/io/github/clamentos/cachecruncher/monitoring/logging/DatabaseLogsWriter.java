package io.github.clamentos.cachecruncher.monitoring.logging;

///
import io.github.clamentos.cachecruncher.persistence.entities.Log;

///..
import io.github.clamentos.cachecruncher.persistence.daos.LogDao;

///.
import java.io.IOException;

///..
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

///..
import java.time.LocalDateTime;
import java.time.ZoneId;

///..
import java.time.format.DateTimeFormatter;

///..
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

///..
import java.util.regex.Pattern;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.scheduling.annotation.Scheduled;

///..
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public class DatabaseLogsWriter {

    ///
    private final LogDao logDao;

    ///..
    private final String logsPath;
    private final int batchSize;

    ///..
    private final Pattern pattern;
    private final DateTimeFormatter formatter;

    ///
    @Autowired
    public DatabaseLogsWriter(LogDao logDao, Environment environment) {

        this.logDao = logDao;
        logsPath = environment.getProperty("cache-cruncher.logsPath", String.class);
        batchSize = environment.getProperty("cache-cruncher.jdbc.batch_size", Integer.class, 64);

        pattern = Pattern.compile("\\|");
        formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
    }

    ///
    @Scheduled(cron = "0 */5 * * * *")
    protected void dump() {

        log.info("Starting logs dumping task...");

        try {

            // Wait for logback to spawn a new file if the previous log triggered a rollover.
            Thread.sleep(1500L);
        }

        catch(InterruptedException exc) {

            Thread.currentThread().interrupt();
            log.warn("Interrupted, ignoring...");
        }

        int[] totalLogsWritten = new int[]{0}; // Used as an "indirect" integer, otherwise lambda complains.
        int totalFilesCleaned = 0;

        try {

            List<Path> paths = Files

                .list(Paths.get(logsPath))
                .filter(path -> !Files.isDirectory(path))
                .sorted((first, second) -> second.getFileName().compareTo(first.getFileName()))
                .skip(1)
                .toList()
            ;

            for(Path path : paths) {

                List<String> lines = new ArrayList<>(batchSize);

                Files.lines(path).forEach(line -> {

                    lines.add(line);
                    totalLogsWritten[0]++;

                    if(lines.size() == batchSize) {

                        this.write(lines);
                        lines.clear();
                    }
                });

                if(!lines.isEmpty()) {

                    this.write(lines);
                }

                Files.delete(path);
                totalFilesCleaned++;
            }
        }

        catch(IOException | RuntimeException exc) {

            log.error("Could not process files, will abort the job", exc);
        }

        log.info("Logs dumping task completed, written {} logs over {} files", totalLogsWritten[0], totalFilesCleaned);
    }

    ///..
    private void write(Collection<String> lines) throws DataAccessException, NullPointerException, NumberFormatException {

        List<Log> logs = new ArrayList<>(lines.size());

        for(String line : lines) {

            String[] sections = pattern.split(line); // section[0] is the initial '|' and can be discarded.
            long timestamp = LocalDateTime.parse(sections[1], formatter).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            LogLevel logLevel = LogLevel.valueOf(sections[2].trim());

            logs.add(new Log(-1, timestamp, sections[3], sections[4], sections[5], logLevel));
        }

        logDao.insert(logs);
    }

    ///
}
