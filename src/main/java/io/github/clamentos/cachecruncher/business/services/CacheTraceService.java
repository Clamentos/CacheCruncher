package io.github.clamentos.cachecruncher.business.services;

///
import com.fasterxml.jackson.core.type.TypeReference;

///.
import io.github.clamentos.cachecruncher.business.validation.CacheTraceValidator;
import io.github.clamentos.cachecruncher.business.validation.SimulationArgumentsValidator;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.SimulationException;
import io.github.clamentos.cachecruncher.error.exceptions.TooManySimulationsException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;
import io.github.clamentos.cachecruncher.utility.Pair;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationReportSummaryDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheSimulationArgumentsDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceDto;

///.
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final SimulationArgumentsValidator simulationArgumentsValidator;
    private final CacheTraceValidator cacheTraceValidator;

    ///..
    private final CacheSimulationService cacheSimulationService;

    ///..
    private final CacheTraceDao cacheTraceDao;

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

        SimulationArgumentsValidator simulationArgumentsValidator,
        CacheTraceValidator cacheTraceValidator,
        CacheSimulationService cacheSimulationService,
        CacheTraceDao cacheTraceDao,
        JsonMapper jsonMapper
    ) {

        this.simulationArgumentsValidator = simulationArgumentsValidator;
        this.cacheTraceValidator = cacheTraceValidator;
        this.cacheSimulationService = cacheSimulationService;
        this.cacheTraceDao = cacheTraceDao;
        this.jsonMapper = jsonMapper;

        completedSimulations = new AtomicLong();
        rejectedSimulations = new AtomicLong();
        cacheTraceBodyDtoType = new TypeReference<>(){};
    }

    ///
    @Transactional
    public void create(CacheTraceDto cacheTraceDto) throws DataAccessException, IllegalArgumentException {

        cacheTraceValidator.validateForCreate(cacheTraceDto);

        CacheTrace newCacheTraceEntity = new CacheTrace(

            System.currentTimeMillis(),
            cacheTraceDto.getDescription(),
            cacheTraceDto.getName(),
            jsonMapper.serialize(cacheTraceDto.getTrace())
        );

        cacheTraceDao.insert(newCacheTraceEntity);
    }

    ///..
    public CacheTraceDto getById(long id) throws DataAccessException, EntityNotFoundException {

        CacheTrace fetchedCacheTraceEntity = cacheTraceDao.selectById(id);

        if(fetchedCacheTraceEntity != null) {

            return new CacheTraceDto(

                fetchedCacheTraceEntity.getId(),
                fetchedCacheTraceEntity.getName(),
                fetchedCacheTraceEntity.getDescription(),
                fetchedCacheTraceEntity.getCreatedAt(),
                fetchedCacheTraceEntity.getUpdatedAt(),
                jsonMapper.deserialize(fetchedCacheTraceEntity.getData(), cacheTraceBodyDtoType)
            );
        }

        throw new EntityNotFoundException(ErrorFactory.create(

            ErrorCode.CACHE_TRACE_NOT_FOUND,
            "CacheTraceService.getById -> Entity does not exist",
            id
        ));
    }

    ///..
    public List<CacheTraceDto> getMinimalByNameLikeAndDates(

        String nameLike,
        long createdAtStart,
        long createdAtEnd,
        long updatedAtStart,
        long updatedAtEnd

    ) throws DataAccessException {

        List<CacheTrace> fetchedCacheTraceEntities = cacheTraceDao.selectMinimalByNameLikeAndDates(

            nameLike + "%",
            createdAtStart,
            createdAtEnd,
            updatedAtStart,
            updatedAtEnd
        );

        List<CacheTraceDto> cacheTraceDtos = new ArrayList<>(fetchedCacheTraceEntities.size());

        for(CacheTrace cacheTraceEntity : fetchedCacheTraceEntities) {

            cacheTraceDtos.add(new CacheTraceDto(

                cacheTraceEntity.getId(),
                cacheTraceEntity.getName(),
                cacheTraceEntity.getDescription(),
                cacheTraceEntity.getCreatedAt(),
                cacheTraceEntity.getUpdatedAt(),
                null
            ));
        }

        return cacheTraceDtos;
    }

    ///..
    @Transactional
    public void update(CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityNotFoundException, IllegalArgumentException {

        cacheTraceValidator.validateForUpdate(cacheTraceDto);
        CacheTrace cacheTraceEntityToUpdate = cacheTraceDao.selectById(cacheTraceDto.getId());

        if(cacheTraceEntityToUpdate == null) {

            throw new EntityNotFoundException(ErrorFactory.create(

                ErrorCode.CACHE_TRACE_NOT_FOUND,
                "CacheTraceService.update -> Entity does not exist",
                cacheTraceDto.getId()
            ));
        }

        if(cacheTraceDto.getName() != null) cacheTraceEntityToUpdate.setName(cacheTraceDto.getName());
        if(cacheTraceDto.getDescription() != null) cacheTraceEntityToUpdate.setDescription(cacheTraceDto.getDescription());
        if(cacheTraceDto.getTrace() != null) cacheTraceEntityToUpdate.setData(jsonMapper.serialize(cacheTraceDto.getTrace()));

        cacheTraceEntityToUpdate.setUpdatedAt(System.currentTimeMillis());
        cacheTraceDao.update(cacheTraceEntityToUpdate);
    }

    ///..
    public void deleteById(long id) throws DataAccessException {

        cacheTraceDao.delete(id);
    }

    ///..
    public Map<String, Map<String, CacheSimulationReportSummaryDto>> simulate(CacheSimulationArgumentsDto simulationArgumentsDto)
    throws DataAccessException, EntityNotFoundException, IllegalArgumentException, SimulationException {

        simulationArgumentsValidator.validate(simulationArgumentsDto);
        Map<String, Map<String, CacheSimulationReportSummaryDto>> combinedReport = new LinkedHashMap<>();

        for(Long traceId : simulationArgumentsDto.getTraceIds()) {

            CacheTrace fetchedCacheTraceEntity = cacheTraceDao.selectById(traceId);

            if(fetchedCacheTraceEntity == null) {

                throw new EntityNotFoundException(ErrorFactory.create(

                    ErrorCode.CACHE_TRACE_NOT_FOUND,
                    "CacheTraceService.simulate -> Entity does not exist",
                    traceId
                ));
            }

            combinedReport.put(fetchedCacheTraceEntity.getName(), new LinkedHashMap<>());
            CacheTraceBodyDto trace = jsonMapper.deserialize(fetchedCacheTraceEntity.getData(), cacheTraceBodyDtoType);
            List<Future<Pair<String, CacheSimulationReportSummaryDto>>> simulations = new ArrayList<>();

            try {

                for(CacheConfigurationDto cacheConfiguration : simulationArgumentsDto.getCacheConfigurations()) {

                    simulations.add(cacheSimulationService.simulate(
    
                        simulationArgumentsDto.getRamAccessTime(),
                        simulationArgumentsDto.getSimulationFlags(),
                        cacheConfiguration,
                        trace
                    ));
                }

                for(Future<Pair<String, CacheSimulationReportSummaryDto>> simulation : simulations) {

                    Pair<String, CacheSimulationReportSummaryDto> simulationResult = simulation.get();
                    combinedReport.get(fetchedCacheTraceEntity.getName()).put(simulationResult.getA(), simulationResult.getB());
                    completedSimulations.incrementAndGet();
                }
            }

            catch(RejectedExecutionException exc) {

                rejectedSimulations.incrementAndGet();

                throw new TooManySimulationsException(ErrorFactory.create(

                    ErrorCode.SERVICE_TEMPORARILY_UNAVAILABLE,
                    "CacheTraceService.simulate -> Simulation queue full"
                ));
            }

            catch(InterruptedException exc) {

                Thread.currentThread().interrupt();
                this.createSimulationException(exc);
            }

            catch(Exception exc) {

                throw this.createSimulationException(exc);
            }
        }

        return combinedReport;
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
    private SimulationException createSimulationException(Exception exc) {

        return new SimulationException(ErrorFactory.create(

            ErrorCode.SIMULATION_ERROR,
            "CacheTraceService.simulate -> " + exc.getMessage()
        ));
    }

    ///
}
