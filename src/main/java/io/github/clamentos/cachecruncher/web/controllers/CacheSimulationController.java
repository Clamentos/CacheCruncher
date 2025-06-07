package io.github.clamentos.cachecruncher.web.controllers;

///
import io.github.clamentos.cachecruncher.business.services.CacheTraceService;

///..
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationSummaryReport;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheSimulationArgumentsDto;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

///..
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping(path = "cache-cruncher")

///
public class CacheSimulationController {

    ///
    private final CacheTraceService cacheTraceService;

    ///
    @Autowired
    public CacheSimulationController(final CacheTraceService cacheTraceService) {

        this.cacheTraceService = cacheTraceService;
    }

    ///
    @GetMapping(path = "/cache-trace/simulation", consumes = "application/json", produces = "application/json")
    public ResponseEntity<SimulationSummaryReport<CacheSimulationRootReportDto>> simulate(

        @RequestBody final CacheSimulationArgumentsDto cacheSimulationArguments

    ) throws ValidationException {

        final SimulationSummaryReport<CacheSimulationRootReportDto> result = cacheTraceService.simulate(cacheSimulationArguments);
        return ResponseEntity.status(result.isHasErrors() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK).body(result);
    }

    ///
}
