package io.github.clamentos.cachecruncher.business.services;

///
import com.fasterxml.jackson.core.type.TypeReference;

///.
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///..
import io.github.clamentos.cachecruncher.business.validation.CacheTraceValidator;
import io.github.clamentos.cachecruncher.business.validation.SimulationArgumentsValidator;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;
import io.github.clamentos.cachecruncher.utility.Pair;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationReport;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationSummaryReport;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheSimulationArgumentsDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceDto;

///.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

///..
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

///..
import java.util.concurrent.atomic.AtomicLong;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.stereotype.Service;

///..
import org.springframework.transaction.annotation.Transactional;

///.
import lombok.extern.slf4j.Slf4j;

///
@Service
@Slf4j

///
public class CacheTraceService {

    ///
    private final CacheSimulationService cacheSimulationService;

    ///..
    private final CacheTraceDao cacheTraceDao;

    ///..
    private final SimulationArgumentsValidator simulationArgumentsValidator;
    private final CacheTraceValidator cacheTraceValidator;

    ///..
    private final JsonMapper jsonMapper;

    ///..
    private final AtomicLong completedSimulations;
    private final AtomicLong rejectedSimulations;

    ///..
    private final TypeReference<CacheTraceBodyDto> cacheTraceBodyDtoType;

    ///
    @Autowired
    public CacheTraceService(

        CacheSimulationService cacheSimulationService,
        CacheTraceDao cacheTraceDao,
        SimulationArgumentsValidator simulationArgumentsValidator,
        CacheTraceValidator cacheTraceValidator,
        JsonMapper jsonMapper
    ) {

        this.cacheSimulationService = cacheSimulationService;
        this.cacheTraceDao = cacheTraceDao;

        this.simulationArgumentsValidator = simulationArgumentsValidator;
        this.cacheTraceValidator = cacheTraceValidator;

        this.jsonMapper = jsonMapper;

        completedSimulations = new AtomicLong();
        rejectedSimulations = new AtomicLong();

        cacheTraceBodyDtoType = new TypeReference<>(){};
    }

    ///
    @Transactional
    public void create(CacheTraceDto cacheTraceDto) throws DataAccessException, IllegalArgumentException {

        cacheTraceValidator.validateForCreate(cacheTraceDto);
        long now = System.currentTimeMillis();

        CacheTrace cacheTraceEntity = new CacheTrace(

            -1L,
            now,
            now,
            cacheTraceDto.getDescription(),
            cacheTraceDto.getName(),
            jsonMapper.serialize(cacheTraceDto.getTrace())
        );

        cacheTraceDao.insert(cacheTraceEntity);
    }

    ///..
    public CacheTraceDto getById(long traceId) throws DataAccessException, EntityNotFoundException {

        CacheTrace cacheTraceEntities = cacheTraceDao.selectById(traceId);

        if(cacheTraceEntities != null) {

            return new CacheTraceDto(

                cacheTraceEntities.getId(),
                cacheTraceEntities.getName(),
                cacheTraceEntities.getDescription(),
                cacheTraceEntities.getCreatedAt(),
                cacheTraceEntities.getUpdatedAt(),
                jsonMapper.deserialize(cacheTraceEntities.getData(), cacheTraceBodyDtoType)
            );
        }

        throw this.createNotFoundException(traceId);
    }

    ///..
    public List<CacheTraceDto> getByFilter(

        String nameLike,
        long createdAtStart,
        long createdAtEnd,
        long updatedAtStart,
        long updatedAtEnd

    ) throws DataAccessException {

        List<CacheTrace> cacheTraceEntities = cacheTraceDao.selectMinimalByNameLikeAndDates(

            nameLike + "%",
            createdAtStart,
            createdAtEnd,
            updatedAtStart,
            updatedAtEnd
        );

        List<CacheTraceDto> cacheTraceDtos = new ArrayList<>(cacheTraceEntities.size());

        for(CacheTrace fetchedCacheTrace : cacheTraceEntities) {

            cacheTraceDtos.add(new CacheTraceDto(

                fetchedCacheTrace.getId(),
                fetchedCacheTrace.getName(),
                fetchedCacheTrace.getDescription(),
                fetchedCacheTrace.getCreatedAt(),
                fetchedCacheTrace.getUpdatedAt(),
                null
            ));
        }

        return cacheTraceDtos;
    }

    ///..
    @Transactional
    public void update(CacheTraceDto cacheTraceDto) throws DataAccessException, EntityNotFoundException, IllegalArgumentException {

        cacheTraceValidator.validateForUpdate(cacheTraceDto);

        Long traceId = cacheTraceDto.getId();
        CacheTrace cacheTraceEntity = cacheTraceDao.selectById(traceId);

        if(cacheTraceEntity == null) {

            throw this.createNotFoundException(traceId);
        }

        if(cacheTraceDto.getName() != null) cacheTraceEntity.setName(cacheTraceDto.getName());
        if(cacheTraceDto.getDescription() != null) cacheTraceEntity.setDescription(cacheTraceDto.getDescription());
        if(cacheTraceDto.getTrace() != null) cacheTraceEntity.setData(jsonMapper.serialize(cacheTraceDto.getTrace()));

        cacheTraceEntity.setUpdatedAt(System.currentTimeMillis());
        cacheTraceDao.update(cacheTraceEntity);
    }

    ///..
    @Transactional
    public void delete(long traceId) throws DataAccessException, EntityNotFoundException {

        if(cacheTraceDao.delete(traceId) == 0) {

            throw this.createNotFoundException(traceId);
        }
    }

    ///..
    public SimulationSummaryReport<CacheSimulationRootReportDto> simulate(CacheSimulationArgumentsDto simulationArgumentsDto)
    throws IllegalArgumentException {

        simulationArgumentsValidator.validate(simulationArgumentsDto);

        boolean hasErrors = false;
        List<Pair<Long, Future<SimulationReport<CacheSimulationRootReportDto>>>> simulations = new ArrayList<>();
        Map<Long, SimulationReport<CacheSimulationRootReportDto>> combinedReport = new HashMap<>();

        for(Long traceId : simulationArgumentsDto.getTraceIds()) {

            try {

                Future<SimulationReport<CacheSimulationRootReportDto>> simulation = cacheSimulationService.simulate(

                    traceId,
                    simulationArgumentsDto.getCacheConfigurations(),
                    simulationArgumentsDto.getSimulationFlags()
                );

                simulations.add(new Pair<>(traceId, simulation));
            }

            catch(RejectedExecutionException _) {

                hasErrors = true;
                rejectedSimulations.incrementAndGet();
                simulations.add(new Pair<>(traceId, null));
            }
        }

        for(Pair<Long, Future<SimulationReport<CacheSimulationRootReportDto>>> simulation : simulations) {

            if(simulation.getB() != null) {

                try {

                    SimulationReport<CacheSimulationRootReportDto> result = simulation.getB().get(10, TimeUnit.SECONDS);
                    combinedReport.put(simulation.getA(), simulation.getB().get(10, TimeUnit.SECONDS));

                    if(!result.getStatus().equals(SimulationStatus.OK)) {

                        hasErrors = true;
                    }

                    else {

                        completedSimulations.incrementAndGet();
                    }
                }

                catch(InterruptedException _) {

                    hasErrors = true;
                    Thread.currentThread().interrupt();
                    combinedReport.put(simulation.getA(), new SimulationReport<>(SimulationStatus.UCATEGORIZED, null));
                }

                catch(TimeoutException _) {

                    hasErrors = true;
                    combinedReport.put(simulation.getA(), new SimulationReport<>(SimulationStatus.REJECTED, null));
                }

                catch(Exception exc) {

                    log.error("Could not simulate", exc);

                    hasErrors = true;
                    combinedReport.put(simulation.getA(), new SimulationReport<>(SimulationStatus.UCATEGORIZED, null));
                }
            }

            else {

                combinedReport.put(simulation.getA(), new SimulationReport<>(SimulationStatus.REJECTED, null));
            }
        }

        return new SimulationSummaryReport<>(hasErrors, combinedReport);
    }

    ///..
    public long getCompletedSimulationCount() {

        return completedSimulations.get();
    }

    ///..
    public long getRejectedSimulationCount() {

        return rejectedSimulations.get();
    }

    ///.
    private EntityNotFoundException createNotFoundException(long traceId) {

        return new EntityNotFoundException(new ErrorDetails(ErrorCode.CACHE_TRACE_NOT_FOUND, traceId));
    }

    ///
}
