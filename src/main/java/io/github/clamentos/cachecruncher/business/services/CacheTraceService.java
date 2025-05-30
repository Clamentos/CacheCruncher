package io.github.clamentos.cachecruncher.business.services;

///
import com.fasterxml.jackson.core.type.TypeReference;

///.
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.CacheCommandArguments;
import io.github.clamentos.cachecruncher.business.simulation.cache.CacheCommandType;

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
import io.github.clamentos.cachecruncher.utility.MutableInt;
import io.github.clamentos.cachecruncher.utility.Pair;
import io.github.clamentos.cachecruncher.utility.Triple;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationReport;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationSummaryReport;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheSimulationArgumentsDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceDto;
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceStatistics;

///.
import java.util.ArrayList;
import java.util.EnumMap;
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

///..
import java.util.stream.Collectors;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

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
    private final long timeout;
    private final int addressRanges;

    ///..
    private final AtomicLong completedSimulations;
    private final AtomicLong rejectedSimulations;

    ///..
    private final TypeReference<CacheTraceBodyDto> cacheTraceBodyDtoType;

    ///
    @Autowired
    public CacheTraceService(

        final CacheSimulationService cacheSimulationService,
        final CacheTraceDao cacheTraceDao,
        final SimulationArgumentsValidator simulationArgumentsValidator,
        final CacheTraceValidator cacheTraceValidator,
        final JsonMapper jsonMapper,
        final Environment environment
    ) {

        this.cacheSimulationService = cacheSimulationService;
        this.cacheTraceDao = cacheTraceDao;

        this.simulationArgumentsValidator = simulationArgumentsValidator;
        this.cacheTraceValidator = cacheTraceValidator;

        this.jsonMapper = jsonMapper;

        timeout = environment.getProperty("cache-cruncher.simulation.executorPool.timeout", Long.class, 10L);
        addressRanges = environment.getProperty("cache-cruncher.cache-trace.statistics.addressRanges", Integer.class, 64);

        completedSimulations = new AtomicLong();
        rejectedSimulations = new AtomicLong();

        cacheTraceBodyDtoType = new TypeReference<>(){};
    }

    ///
    @Transactional
    public void create(final CacheTraceDto cacheTraceDto) throws DataAccessException, IllegalArgumentException {

        cacheTraceValidator.validateForCreate(cacheTraceDto);

        CacheTraceBodyDto trace = cacheTraceDto.getTrace();
        this.calculateTraceStatistics(trace);

        CacheTrace cacheTraceEntity = new CacheTrace(

            -1L,
            System.currentTimeMillis(),
            null,
            cacheTraceDto.getDescription(),
            cacheTraceDto.getName(),
            jsonMapper.serialize(trace)
        );

        cacheTraceDao.insert(cacheTraceEntity);
    }

    ///..
    public CacheTraceDto getById(final long traceId) throws DataAccessException, EntityNotFoundException {

        final CacheTrace cacheTraceEntities = cacheTraceDao.selectById(traceId);

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

        final String nameLike,
        final long createdAtStart,
        final long createdAtEnd,
        final Long updatedAtStart,
        final Long updatedAtEnd

    ) throws DataAccessException {

        if(updatedAtStart == null) cacheTraceValidator.requireNull(updatedAtEnd, "updatedAtEnd");
        else cacheTraceValidator.requireNotNull(updatedAtEnd, "updatedAtEnd");

        final boolean requiresUpdated = updatedAtStart != null;
        List<CacheTrace> cacheTraceEntities;

        if(requiresUpdated) {

            cacheTraceEntities = cacheTraceDao.selectMinimalByNameLikeAndDates(

                nameLike + "%",
                createdAtStart,
                createdAtEnd,
                updatedAtStart,
                updatedAtEnd
            );
        }

        else {

            cacheTraceEntities = cacheTraceDao.selectMinimalByNameLikeAndDate(nameLike + "%", createdAtStart, createdAtEnd);
        }

        final List<CacheTraceDto> cacheTraceDtos = new ArrayList<>(cacheTraceEntities.size());

        for(final CacheTrace fetchedCacheTrace : cacheTraceEntities) {

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
    public void update(final CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityNotFoundException, IllegalArgumentException {

        cacheTraceValidator.validateForUpdate(cacheTraceDto);

        final Long traceId = cacheTraceDto.getId();
        final CacheTrace cacheTraceEntity = cacheTraceDao.selectById(traceId);

        if(cacheTraceEntity == null) throw this.createNotFoundException(traceId);

        if(cacheTraceDto.getName() != null) cacheTraceEntity.setName(cacheTraceDto.getName());
        if(cacheTraceDto.getDescription() != null) cacheTraceEntity.setDescription(cacheTraceDto.getDescription());
        if(cacheTraceDto.getTrace() != null) cacheTraceEntity.setData(jsonMapper.serialize(cacheTraceDto.getTrace()));

        cacheTraceEntity.setUpdatedAt(System.currentTimeMillis());
        cacheTraceDao.update(cacheTraceEntity);
    }

    ///..
    @Transactional
    public void delete(final long traceId) throws DataAccessException, EntityNotFoundException {

        if(cacheTraceDao.delete(traceId) == 0L) throw this.createNotFoundException(traceId);
    }

    ///..
    public SimulationSummaryReport<CacheSimulationRootReportDto> simulate(final CacheSimulationArgumentsDto simulationArgumentsDto)
    throws IllegalArgumentException {

        simulationArgumentsValidator.validate(simulationArgumentsDto);

        boolean hasErrors = false;
        final List<Pair<Long, Future<SimulationReport<CacheSimulationRootReportDto>>>> simulations = new ArrayList<>();
        final Map<Long, SimulationReport<CacheSimulationRootReportDto>> combinedReport = new HashMap<>();

        hasErrors |= this.launchSimulations(simulationArgumentsDto, simulations, combinedReport);
        hasErrors |= this.waitForResults(simulations, combinedReport);

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
    private void calculateTraceStatistics(final CacheTraceBodyDto cacheTraceBody) {

        final Map<CacheCommandType, MutableInt> commandDistribution = new EnumMap<>(CacheCommandType.class);
        final List<Triple<Long, Long, MutableInt>> addressDistribution = new ArrayList<>(addressRanges);
        final Map<String, MutableInt> repeatMap = new HashMap<>();

        final long increment = Long.MAX_VALUE / addressRanges;
        long start = 0L;

        for(int i = 0; i < addressRanges; i++) {

            addressDistribution.add(new Triple<>(start, start + increment, new MutableInt()));
            start += (increment + 1);
        }

        this.incrementTraceStatistics(cacheTraceBody.getBody(), commandDistribution, addressDistribution, repeatMap, 1);

        for(final Map.Entry<String, MutableInt> repetitions : repeatMap.entrySet()) {

            this.incrementTraceStatistics(

                cacheTraceBody.getSections().getAll(repetitions.getKey()),
                commandDistribution,
                addressDistribution,
                null,
                repetitions.getValue().getValue()
            );
        }

        cacheTraceBody.setStatistics(new CacheTraceStatistics(

            commandDistribution.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getValue())),

            addressDistribution.stream().collect(Collectors.toMap(

                k -> (Long.toHexString(k.getA()) + "-" + Long.toHexString(k.getB())),
                v -> v.getC().getValue()
            ))
        ));
    }

    ///..
    private EntityNotFoundException createNotFoundException(final long traceId) {

        return new EntityNotFoundException(new ErrorDetails(ErrorCode.CACHE_TRACE_NOT_FOUND, traceId));
    }

    ///..
    private boolean launchSimulations(

        final CacheSimulationArgumentsDto simulationArgumentsDto,
        final List<Pair<Long, Future<SimulationReport<CacheSimulationRootReportDto>>>> simulations,
        final Map<Long, SimulationReport<CacheSimulationRootReportDto>> combinedReport
    ) {

        boolean hasErrors = false;

        for(final Long traceId : simulationArgumentsDto.getTraceIds()) {

            try {

                final Future<SimulationReport<CacheSimulationRootReportDto>> simulation = cacheSimulationService.simulate(

                    traceId,
                    simulationArgumentsDto.getCacheConfigurations(),
                    simulationArgumentsDto.getSimulationFlags()
                );

                simulations.add(new Pair<>(traceId, simulation));
            }

            catch(RejectedExecutionException _) {

                hasErrors = true;
                rejectedSimulations.incrementAndGet();
                combinedReport.put(traceId, new SimulationReport<>(SimulationStatus.REJECTED, null));
            }
        }

        return hasErrors;
    }

    ///..
    private boolean waitForResults(

        final List<Pair<Long, Future<SimulationReport<CacheSimulationRootReportDto>>>> simulations,
        final Map<Long, SimulationReport<CacheSimulationRootReportDto>> combinedReport
    ) {

        boolean hasErrors = false;

        for(final Pair<Long, Future<SimulationReport<CacheSimulationRootReportDto>>> simulation : simulations) {

            try {

                final SimulationReport<CacheSimulationRootReportDto> result = simulation.getB().get(timeout, TimeUnit.SECONDS);
                combinedReport.put(simulation.getA(), result);

                if(!result.getStatus().equals(SimulationStatus.OK)) hasErrors = true;
                else completedSimulations.incrementAndGet();
            }

            catch(final Exception exc) {

                hasErrors = true;

                if(exc instanceof InterruptedException) Thread.currentThread().interrupt();

                if(exc instanceof TimeoutException) {

                    combinedReport.put(simulation.getA(), new SimulationReport<>(SimulationStatus.REJECTED, null));
                }

                else {

                    log.error("Could not simulate", exc);
                    combinedReport.put(simulation.getA(), new SimulationReport<>(SimulationStatus.UCATEGORIZED, null));
                }
            }
        }

        return hasErrors;
    }

    ///..
    private void incrementTraceStatistics(

        final List<String> commands,
        final Map<CacheCommandType, MutableInt> commandDistribution,
        final List<Triple<Long, Long, MutableInt>> addressDistribution,
        final Map<String, MutableInt> repeatMap,
        final int quantity
    ) {

        for(final String command : commands) {

            final CacheCommandType commandType = CacheCommandType.determineType(command);
            commandDistribution.computeIfAbsent(commandType, _ -> new MutableInt()).incrementAndGet(quantity);

            if(commandType == CacheCommandType.READ || commandType == CacheCommandType.WRITE) {

                final CacheCommandArguments arguments = cacheSimulationService.parseReadWritePrefetch(command);
                final int size = arguments.getSize();
                final long address = arguments.getAddress();

                for(int i = 0; i < size; i++) {

                    this.incrementAddressDistribution(addressDistribution, address + i, quantity);
                }
            }

            if(commandType == CacheCommandType.REPEAT && repeatMap != null) {

                final String[] commandComponents = command.split("#");
                final int repetitions = Integer.parseInt(commandComponents[1]);

                repeatMap.computeIfAbsent(commandComponents[2], _ -> new MutableInt()).incrementAndGet(repetitions);
            }
        }
    }

    ///..
    private void incrementAddressDistribution(

        final List<Triple<Long, Long, MutableInt>> addressDistribution,
        final long address,
        final int quantity
    ) {

        for(final Triple<Long, Long, MutableInt> entry : addressDistribution) {

            if(entry.getA().compareTo(address) <= 0 && entry.getB().compareTo(address) >= 0) {

                entry.getC().incrementAndGet(quantity);
                break;
            }
        }
    }

    ///
}
