package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.monitoring.status.ApplicationStatusService;

///..
import io.github.clamentos.cachecruncher.web.dtos.status.ApplicationStatusDto;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.http.ResponseEntity;

///..
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping(path = "cache-cruncher/status")

///
public class ApplicationStatusController { // TODO: cruds for logs

    ///
    private final ApplicationStatusService applicationStatusService;

    ///
    @Autowired
    public ApplicationStatusController(ApplicationStatusService applicationStatusService) {

        this.applicationStatusService = applicationStatusService;
    }

    ///
    @GetMapping(produces = "application/json")
    public ResponseEntity<ApplicationStatusDto> getStatistics(

        @RequestParam(required = false) Boolean includeRuntimeInfo,
        @RequestParam(required = false) Boolean includeMemoryInfo,
        @RequestParam(required = false) Boolean includeThreadsInfo,
        @RequestParam(required = false) Boolean includeResponsesInfo,
        @RequestParam(required = false) Boolean includeSimulationInfo
    ) {

        return ResponseEntity.ok(applicationStatusService.getStatistics(

            includeRuntimeInfo,
            includeMemoryInfo,
            includeThreadsInfo,
            includeResponsesInfo,
            includeSimulationInfo
        ));
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> resetStatistics() {

        applicationStatusService.resetStatistics();
        return ResponseEntity.ok().build();
    }

    ///
}
