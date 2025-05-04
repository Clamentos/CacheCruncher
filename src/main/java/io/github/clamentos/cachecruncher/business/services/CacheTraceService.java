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
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationReport;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationSummaryReport;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;
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

        CacheTrace cacheTraceEntity = new CacheTrace(

            System.currentTimeMillis(),
            cacheTraceDto.getDescription(),
            cacheTraceDto.getName(),
            jsonMapper.serialize(cacheTraceDto.getTrace())
        );

        cacheTraceDao.insert(cacheTraceEntity);
    }

    ///..
    public CacheTraceDto getById(long id) throws DataAccessException, EntityNotFoundException {

        CacheTrace cacheTraceEntities = cacheTraceDao.selectById(id);

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

        throw this.createNotFoundException("getById", id);
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

        Long id = cacheTraceDto.getId();
        CacheTrace cacheTraceEntity = cacheTraceDao.selectById(id);

        if(cacheTraceEntity == null) {

            throw this.createNotFoundException("update", id);
        }

        if(cacheTraceDto.getName() != null) cacheTraceEntity.setName(cacheTraceDto.getName());
        if(cacheTraceDto.getDescription() != null) cacheTraceEntity.setDescription(cacheTraceDto.getDescription());
        if(cacheTraceDto.getTrace() != null) cacheTraceEntity.setData(jsonMapper.serialize(cacheTraceDto.getTrace()));

        cacheTraceEntity.setUpdatedAt(System.currentTimeMillis());
        cacheTraceDao.update(cacheTraceEntity);
    }

    ///..
    @Transactional
    public void delete(long id) throws DataAccessException, EntityNotFoundException {

        if(cacheTraceDao.delete(id) == 0) {

            throw this.createNotFoundException("delete", id);
        }
    }

    ///..
    public SimulationSummaryReport<CacheSimulationRootReportDto> simulate(CacheSimulationArgumentsDto simulationArgumentsDto)
    throws IllegalArgumentException {

        simulationArgumentsValidator.validate(simulationArgumentsDto);

        boolean hasErrors = false;
        Map<Long, SimulationReport<CacheSimulationRootReportDto>> combinedReport = new HashMap<>();

        for(Long traceId : simulationArgumentsDto.getTraceIds()) {

            CacheTrace cacheTraceEntity = null;

            try {

                cacheTraceEntity = cacheTraceDao.selectById(traceId);
            }

            catch(DataAccessException exc) {

                log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());

                combinedReport.put(traceId, new SimulationReport<>(SimulationStatus.UCATEGORIZED, null));
                hasErrors = true;

                continue;
            }

            if(cacheTraceEntity != null) {

                CacheTraceBodyDto trace = jsonMapper.deserialize(cacheTraceEntity.getData(), cacheTraceBodyDtoType);
                List<Future<CacheSimulationRootReportDto>> simulations = new ArrayList<>();

                for(CacheConfigurationDto cacheConfiguration : simulationArgumentsDto.getCacheConfigurations()) {

                    String cacheConfigurationName = cacheConfiguration.getName();

                    try {

                        simulations.add(cacheSimulationService.simulate(

                            simulationArgumentsDto.getRamAccessTime(),
                            simulationArgumentsDto.getSimulationFlags(),
                            cacheConfiguration,
                            trace
                        ));
                    }

                    catch(RejectedExecutionException exc) {

                        rejectedSimulations.incrementAndGet();
                        hasErrors = true;

                        this.addReport(

                            combinedReport,
                            CacheSimulationRootReportDto.newRejected(),
                            cacheConfigurationName,
                            SimulationStatus.NESTED_ERRORS,
                            traceId
                        );
                    }

                    for(Future<CacheSimulationRootReportDto> simulation : simulations) {

                        try {

                            CacheSimulationRootReportDto simulationResult = simulation.get();
                            completedSimulations.incrementAndGet();
                            this.addReport(combinedReport, simulationResult, cacheConfigurationName, SimulationStatus.OK, traceId);
                        }

                        catch(InterruptedException exc) {

                            Thread.currentThread().interrupt();
                            this.handleSimulationException(combinedReport, cacheConfigurationName, traceId);
                            hasErrors = true;
                        }
    
                        catch(Exception exc) {
    
                            log.error("Could not simulate", exc);
                            this.handleSimulationException(combinedReport, cacheConfigurationName, traceId);
                            hasErrors = true;
                        }
                    }
                }
            }

            else {

                combinedReport.put(traceId, new SimulationReport<>(SimulationStatus.NOT_FOUND, null));
                hasErrors = true;
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
    private EntityNotFoundException createNotFoundException(String methodName, long id) {

        return new EntityNotFoundException(ErrorFactory.create(

            ErrorCode.CACHE_TRACE_NOT_FOUND,
            "CacheTraceService." + methodName + " -> The specified cache trace does not exist",
            id
        ));
    }

    ///..
    private void addReport(

        Map<Long, SimulationReport<CacheSimulationRootReportDto>> combinedReport,
        CacheSimulationRootReportDto newSummary,
        String cacheConfigurationName,
        SimulationStatus status,
        Long traceId
    ) {

        SimulationReport<CacheSimulationRootReportDto> traceReport = combinedReport.get(traceId);

        if(traceReport == null) {

            Map<String, CacheSimulationRootReportDto> reportMap = new HashMap<>();

            reportMap.put(cacheConfigurationName, newSummary);
            combinedReport.put(traceId, new SimulationReport<>(status, reportMap));
        }

        else {

            traceReport.getReport().put(cacheConfigurationName, newSummary);
        }
    }

    ///..
    private void handleSimulationException(

        Map<Long, SimulationReport<CacheSimulationRootReportDto>> combinedReport,
        String cacheConfigurationName,
        Long traceId
    ) {
        this.addReport(combinedReport, null, cacheConfigurationName, SimulationStatus.UCATEGORIZED, traceId);
    }

    ///
}
