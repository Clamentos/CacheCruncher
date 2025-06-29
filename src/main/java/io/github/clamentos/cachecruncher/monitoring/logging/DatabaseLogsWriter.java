package io.github.clamentos.cachecruncher.monitoring.logging;

///
import io.github.clamentos.cachecruncher.persistence.entities.Log;

///..
import io.github.clamentos.cachecruncher.utility.MutableInt;
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///..
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

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
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;
import org.springframework.dao.NonTransientDataAccessResourceException;

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
    private final Pattern pattern;
    private final DateTimeFormatter formatter;
    private final ZoneId zoneId;

    ///..
    private final String logsPath;

    ///
    @Autowired
    public DatabaseLogsWriter(final LogDao logDao, final PropertyProvider propertyProvider) throws BeanCreationException {

        this.logDao = logDao;

        pattern = Pattern.compile("\\|");
        formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
        zoneId = ZoneId.systemDefault();

        logsPath = propertyProvider.getString("cache-cruncher.logsPath", null);
    }

    ///
    public void dumpTask() {

        log.info("Starting logs dumping task...");

        try {

            // Wait for logback to spawn a new file if the previous log triggered a rollover.
            Thread.sleep(1_500L);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            log.warn("Interrupted, ignoring...");
        }

        final MutableInt totalLogsWritten = new MutableInt();
        final int batchSize = logDao.getBatchSize();
        int totalFilesCleaned = 0;

        try {

            final List<Path> paths = Files

                .list(Paths.get(logsPath))
                .filter(path -> !Files.isDirectory(path))
                .sorted((first, second) -> second.getFileName().compareTo(first.getFileName()))
                .skip(1L)
                .toList()
            ;

            for(final Path path : paths) {

                final List<String> lines = new ArrayList<>(batchSize);

                Files.lines(path).forEach(line -> {

                    lines.add(line);
                    totalLogsWritten.incrementAndGet(1);

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

        catch(final IOException | RuntimeException exc) {

            log.error("Could not process files, will abort the job", exc);
        }

        log.info("Logs dumping task completed, {} logs written over {} files", totalLogsWritten.getValue(), totalFilesCleaned);
    }

    ///.
    private void write(final Collection<String> lines) throws DataAccessException {

        final List<Log> logs = new ArrayList<>(lines.size());

        for(final String line : lines) {

            final String[] sections = pattern.split(line); // section[0] is the initial '|' and can be discarded.
            final long timestamp = LocalDateTime.parse(sections[1], formatter).atZone(zoneId).toInstant().toEpochMilli();
            final LogLevel logLevel = LogLevel.valueOf(sections[2].trim());

            logs.add(new Log(-1L, timestamp, sections[3], sections[4], sections[5], logLevel));
        }

        try { logDao.insert(logs); }
        catch(final DatabaseException exc) { throw new NonTransientDataAccessResourceException("Unwrapped", exc.getCause()); }
    }

    ///
}
