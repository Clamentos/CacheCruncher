package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandType;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeB;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeC;

///..
import io.github.clamentos.cachecruncher.business.validation.CacheTraceValidator;
import io.github.clamentos.cachecruncher.business.validation.SimulationArgumentsValidator;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.CacheCruncherException;
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.SerializationException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;
import io.github.clamentos.cachecruncher.utility.MutableInt;
import io.github.clamentos.cachecruncher.utility.Pair;
import io.github.clamentos.cachecruncher.utility.PropertyProvider;
import io.github.clamentos.cachecruncher.utility.Triple;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationReport;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationSummaryReport;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheSimulationArgumentsDto;

///..
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
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

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

    ///
    @Autowired
    public CacheTraceService(

        final CacheSimulationService cacheSimulationService,
        final CacheTraceDao cacheTraceDao,
        final SimulationArgumentsValidator simulationArgumentsValidator,
        final CacheTraceValidator cacheTraceValidator,
        final JsonMapper jsonMapper,
        final PropertyProvider propertyProvider

    ) throws BeanCreationException {

        this.cacheSimulationService = cacheSimulationService;
        this.cacheTraceDao = cacheTraceDao;

        this.simulationArgumentsValidator = simulationArgumentsValidator;
        this.cacheTraceValidator = cacheTraceValidator;

        this.jsonMapper = jsonMapper;

        timeout = propertyProvider.getLong("cache-cruncher.simulation.executorPool.timeout", 10_000L, 500L, Long.MAX_VALUE);
        addressRanges = propertyProvider.getInteger("cache-cruncher.cache-trace.statistics.addressRanges", 64, 1, Integer.MAX_VALUE);

        completedSimulations = new AtomicLong();
        rejectedSimulations = new AtomicLong();
    }

    ///
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void create(final CacheTraceDto cacheTraceDto) throws DatabaseException, SerializationException, ValidationException {

        cacheTraceValidator.validateForCreate(cacheTraceDto);
        final CacheTraceBody trace = cacheTraceDto.getTrace();

        final CacheTrace cacheTraceEntity = new CacheTrace(

            -1L,
            System.currentTimeMillis(),
            null,
            cacheTraceDto.getDescription(),
            cacheTraceDto.getName(),
            jsonMapper.serialize(this.calculateTraceStatistics(trace)),
            cacheTraceDto.getTrace()
        );

        cacheTraceDao.insert(cacheTraceEntity);
    }

    ///..
    public CacheTraceDto getById(final long traceId) throws DatabaseException, DeserializationException, EntityNotFoundException {

        final CacheTrace cacheTraceEntity = cacheTraceDao.selectById(traceId);
        if(cacheTraceEntity != null) return new CacheTraceDto(cacheTraceEntity, jsonMapper);

        throw new EntityNotFoundException(ErrorCode.CACHE_TRACE_NOT_FOUND, traceId);
    }

    ///..
    public List<CacheTraceDto> getByFilter(

        final String nameLike,
        final long createdAtStart,
        final long createdAtEnd,
        final Long updatedAtStart,
        final Long updatedAtEnd

    ) throws DatabaseException, DeserializationException, ValidationException {

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

            cacheTraceDtos.add(new CacheTraceDto(fetchedCacheTrace, jsonMapper));
        }

        return cacheTraceDtos;
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void update(final CacheTraceDto cacheTraceDto)
    throws DatabaseException, EntityNotFoundException, SerializationException, ValidationException {

        cacheTraceValidator.validateForUpdate(cacheTraceDto);

        final Long traceId = cacheTraceDto.getId();
        final CacheTrace cacheTraceEntity = cacheTraceDao.selectById(traceId);

        if(cacheTraceEntity == null) throw new EntityNotFoundException(ErrorCode.CACHE_TRACE_NOT_FOUND, traceId);

        if(cacheTraceDto.getName() != null) cacheTraceEntity.setName(cacheTraceDto.getName());
        if(cacheTraceDto.getDescription() != null) cacheTraceEntity.setDescription(cacheTraceDto.getDescription());

        if(cacheTraceDto.getTrace() != null) {

            cacheTraceEntity.setStatistics(jsonMapper.serialize(this.calculateTraceStatistics(cacheTraceDto.getTrace())));
            cacheTraceEntity.setTrace(cacheTraceDto.getTrace());
        }

        cacheTraceEntity.setUpdatedAt(System.currentTimeMillis());
        cacheTraceDao.update(cacheTraceEntity);
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void delete(final long traceId) throws DatabaseException, EntityNotFoundException {

        if(cacheTraceDao.delete(traceId) == 0L) throw new EntityNotFoundException(ErrorCode.CACHE_TRACE_NOT_FOUND, traceId);
    }

    ///..
    public SimulationSummaryReport<CacheSimulationRootReportDto> simulate(final CacheSimulationArgumentsDto simulationArgumentsDto)
    throws ValidationException {

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
    private CacheTraceStatistics calculateTraceStatistics(final CacheTraceBody cacheTraceBody) {

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

        return new CacheTraceStatistics(

            commandDistribution.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getValue())),

            addressDistribution.stream().collect(Collectors.toMap(

                k -> (Long.toHexString(k.getA()) + "-" + Long.toHexString(k.getB())),
                v -> v.getC().getValue()
            ))
        );
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

            catch(final RejectedExecutionException _) {

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

                final SimulationReport<CacheSimulationRootReportDto> result = simulation.getB().get(timeout, TimeUnit.MILLISECONDS);
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

        final List<CacheCommand> commands,
        final Map<CacheCommandType, MutableInt> commandDistribution,
        final List<Triple<Long, Long, MutableInt>> addressDistribution,
        final Map<String, MutableInt> repeatMap,
        final int quantity
    ) {

        for(final CacheCommand command : commands) {

            commandDistribution.computeIfAbsent(command.getType(), _ -> new MutableInt()).incrementAndGet(quantity);

            if(command instanceof final CacheCommandTypeB commandTypeB) {

                for(int i = 0; i < commandTypeB.getSize(); i++) {

                    this.incrementAddressDistribution(addressDistribution, commandTypeB.getValue() + i, quantity);
                }
            }

            if((command instanceof final CacheCommandTypeC commandTypeC) && repeatMap != null) {

                repeatMap.computeIfAbsent(commandTypeC.getSectionName(), _ -> new MutableInt())

                    .incrementAndGet(commandTypeC.getRepeats())
                ;
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
