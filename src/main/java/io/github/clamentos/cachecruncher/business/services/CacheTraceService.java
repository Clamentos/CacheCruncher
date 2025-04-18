package io.github.clamentos.cachecruncher.business.services;

///
import com.fasterxml.jackson.core.type.TypeReference;

///..
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.clamentos.cachecruncher.business.validation.CacheTraceValidator;
///.
import io.github.clamentos.cachecruncher.business.validation.SimulationArgumentsValidator;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.SimulationException;
import io.github.clamentos.cachecruncher.error.exceptions.TooManySimulationsException;
///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;

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
import java.util.Map.Entry;

///..
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
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
    private final CacheTraceDao cacheTraceDao;
    private final ObjectMapper objectMapper;

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
        ObjectMapper objectMapper
    ) {

        this.simulationArgumentsValidator = simulationArgumentsValidator;
        this.cacheTraceValidator = cacheTraceValidator;
        this.cacheSimulationService = cacheSimulationService;
        this.cacheTraceDao = cacheTraceDao;
        this.objectMapper = objectMapper;

        completedSimulations = new AtomicLong();
        rejectedSimulations = new AtomicLong();
        cacheTraceBodyDtoType = new TypeReference<CacheTraceBodyDto>(){};
    }

    ///
    @Transactional
    public void create(CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityAlreadyExistsException, IllegalArgumentException {

        cacheTraceValidator.validateForCreate(cacheTraceDto);

        if(cacheTraceDao.existsByName(cacheTraceDto.getName())) {

            throw new EntityAlreadyExistsException(ErrorFactory.create(

                ErrorCode.CACHE_TRACE_ALREADY_EXISTS,
                "CacheTraceService.create -> Entity already exists",
                cacheTraceDto.getName()
            ));
        }

        CacheTrace entity = new CacheTrace(

            System.currentTimeMillis(),
            cacheTraceDto.getDescription(),
            cacheTraceDto.getName(),
            JsonMapper.serialize(cacheTraceDto.getData(), objectMapper)
        );

        cacheTraceDao.insert(entity);
    }

    ///..
    public CacheTraceDto getById(long id) throws DataAccessException, EntityNotFoundException {

        CacheTrace entity = cacheTraceDao.selectById(id);

        if(entity != null) {

            return new CacheTraceDto(

                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                JsonMapper.deserialize(entity.getData(), cacheTraceBodyDtoType, objectMapper)
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

        List<CacheTrace> entities = cacheTraceDao.selectMinimalByNameLikeAndDates(

            "%" + nameLike + "%",
            createdAtStart,
            createdAtEnd,
            updatedAtStart,
            updatedAtEnd
        );

        List<CacheTraceDto> dtos = new ArrayList<>(entities.size());

        for(CacheTrace entity : entities) {

            dtos.add(new CacheTraceDto(

                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null
            ));
        }

        return dtos;
    }

    ///..
    @Transactional
    public void update(CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityAlreadyExistsException, EntityNotFoundException, IllegalArgumentException {

        cacheTraceValidator.validateForUpdate(cacheTraceDto);
        CacheTrace entity = cacheTraceDao.selectById(cacheTraceDto.getId());

        if(entity == null) {

            throw new EntityNotFoundException(ErrorFactory.create(

                ErrorCode.CACHE_TRACE_NOT_FOUND,
                "CacheTraceService.update -> Entity does not exist",
                cacheTraceDto.getId()
            ));
        }

        if(cacheTraceDto.getName() != null) {

            if(cacheTraceDao.existsByName(cacheTraceDto.getName())) {

                throw new EntityAlreadyExistsException(ErrorFactory.create(

                    ErrorCode.CACHE_TRACE_ALREADY_EXISTS,
                    "CacheTraceService.update -> Entity already exists",
                    cacheTraceDto.getName()
                ));
            }

            entity.setName(cacheTraceDto.getName());
        }

        if(cacheTraceDto.getDescription() != null) entity.setDescription(cacheTraceDto.getDescription());

        if(cacheTraceDto.getData() != null) {

            entity.setData(JsonMapper.serialize(cacheTraceDto.getData(), objectMapper));
        }

        entity.setUpdatedAt(System.currentTimeMillis());
        cacheTraceDao.update(entity);
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

            CacheTrace entity = cacheTraceDao.selectById(traceId);

            if(entity == null) {

                throw new EntityNotFoundException(ErrorFactory.create(

                    ErrorCode.CACHE_TRACE_NOT_FOUND,
                    "CacheTraceService.simulate -> Entity does not exist",
                    traceId
                ));
            }

            combinedReport.put(entity.getName(), new LinkedHashMap<>());
            CacheTraceBodyDto trace = JsonMapper.deserialize(entity.getData(), cacheTraceBodyDtoType, objectMapper);
            List<Future<Entry<String, CacheSimulationReportSummaryDto>>> simulations = new ArrayList<>();

            try {

                for(CacheConfigurationDto cacheConfiguration : simulationArgumentsDto.getCacheConfigurations()) {

                    simulations.add(cacheSimulationService.simulate(
    
                        simulationArgumentsDto.getRamAccessTime(),
                        simulationArgumentsDto.getSimulationFlags(),
                        cacheConfiguration,
                        trace
                    ));
                }

                for(Future<Entry<String, CacheSimulationReportSummaryDto>> simulation : simulations) {

                    Entry<String, CacheSimulationReportSummaryDto> result = simulation.get();
                    combinedReport.get(entity.getName()).put(result.getKey(), result.getValue());
                    completedSimulations.incrementAndGet();
                }
            }

            catch(InterruptedException exc) {

                Thread.currentThread().interrupt();
                createSimulationException(exc);
            }

            catch(RejectedExecutionException exc) {

                rejectedSimulations.incrementAndGet();

                throw new TooManySimulationsException(ErrorFactory.create(

                    ErrorCode.SERVICE_TEMPORARILY_UNAVAILABLE,
                    "CacheTraceService.simulate -> Simulation queue full"
                ));
            }

            catch(Exception exc) {

                exc.printStackTrace();
                throw createSimulationException(exc);
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
