package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.monitoring.logging.LogLevel;

///..
import io.github.clamentos.cachecruncher.monitoring.status.ApplicationStatusService;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Log;

///..
import io.github.clamentos.cachecruncher.web.dtos.filters.LogSearchFilter;
import io.github.clamentos.cachecruncher.web.dtos.filters.ResponseInfoSearchFilter;

///..
import io.github.clamentos.cachecruncher.web.dtos.status.ApplicationStatusDto;
import io.github.clamentos.cachecruncher.web.dtos.status.ResponsesInfo;

///.
import java.util.List;
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.http.ResponseEntity;

///..
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping(path = "cache-cruncher/status")

///
public class ApplicationStatusController {

    ///
    private final ApplicationStatusService applicationStatusService;

    ///
    @Autowired
    public ApplicationStatusController(final ApplicationStatusService applicationStatusService) {

        this.applicationStatusService = applicationStatusService;
    }

    ///
    @GetMapping(path = "/metrics", produces = "application/json")
    public ResponseEntity<ApplicationStatusDto> getStatistics(

        @RequestParam final boolean includeRuntimeInfo,
        @RequestParam final boolean includeMemoryInfo,
        @RequestParam final boolean includeThreadsInfo,
        @RequestParam final boolean includeResponsesInfo,
        @RequestParam final boolean includeSimulationInfo,
        @RequestParam final boolean includeSessionsInfo
    ) {

        return ResponseEntity.ok(applicationStatusService.getStatistics(

            includeRuntimeInfo,
            includeMemoryInfo,
            includeThreadsInfo,
            includeResponsesInfo,
            includeSimulationInfo,
            includeSessionsInfo
        ));
    }

    ///..
    @GetMapping(path = "/metrics/history", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ResponsesInfo> getResponsesInfoByFilter(@RequestBody final ResponseInfoSearchFilter responseInfoSearchFilter)
    throws DataAccessException, IllegalArgumentException {

        return ResponseEntity.ok(applicationStatusService.getResponsesInfoByFilter(responseInfoSearchFilter));
    }

    ///..
    @GetMapping(path = "/logs", consumes = "application/json", produces = "application/json")
    public ResponseEntity<List<Log>> getLogsByFilter(@RequestBody final LogSearchFilter logSearchFilter)
    throws DataAccessException, IllegalArgumentException {

        return ResponseEntity.ok(applicationStatusService.getLogsByFilter(logSearchFilter));
    }

    ///..
    @GetMapping(path = "/logs/count", produces = "application/json")
    public ResponseEntity<Map<LogLevel, Long>> getLogsCount() throws DataAccessException {

        return ResponseEntity.ok(applicationStatusService.getLogsCount());
    }

    ///..
    @DeleteMapping(path = "/metrics/history")
    public ResponseEntity<Integer> deleteMetrics(@RequestParam final long createdAtStart, @RequestParam final long createdAtEnd)
    throws DataAccessException {

        return ResponseEntity.ok(applicationStatusService.deleteMetrics(createdAtStart, createdAtEnd));
    }

    ///..
    @DeleteMapping(path = "/logs")
    public ResponseEntity<Integer> deleteLogs(@RequestParam final long createdAtStart, @RequestParam final long createdAtEnd)
    throws DataAccessException {

        return ResponseEntity.ok(applicationStatusService.deleteLogs(createdAtStart, createdAtEnd));
    }

    ///
}
