package io.github.clamentos.cachecruncher.business.services;

///
import com.fasterxml.jackson.core.type.TypeReference;

///.
import io.github.clamentos.cachecruncher.business.simulation.SimulationFlag;
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.Cache;
import io.github.clamentos.cachecruncher.business.simulation.cache.CacheCommandArguments;
import io.github.clamentos.cachecruncher.business.simulation.cache.CacheCommandType;
import io.github.clamentos.cachecruncher.business.simulation.cache.Memory;
import io.github.clamentos.cachecruncher.business.simulation.cache.SystemMemory;

///..
import io.github.clamentos.cachecruncher.business.simulation.event.EventManager;

///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationReport;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;
import io.github.clamentos.cachecruncher.web.dtos.simulation.MemoryConfigurationDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceBodyDto;

///.
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

///..
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.scheduling.annotation.Async;

///..
import org.springframework.stereotype.Service;

///
@Service
@Slf4j

///
public class CacheSimulationService {

    ///
    private final CacheTraceDao cacheTraceDao;
    private final JsonMapper jsonMapper;

    ///..
    private final TypeReference<CacheTraceBodyDto> cacheTraceBodyDtoType;

    ///
    @Autowired
    public CacheSimulationService(final CacheTraceDao cacheTraceDao, final JsonMapper jsonMapper) {

        this.cacheTraceDao = cacheTraceDao;
        this.jsonMapper = jsonMapper;

        cacheTraceBodyDtoType = new TypeReference<>(){};
    }

    ///
    @Async(value = "simulationsExecutor")
    public Future<SimulationReport<CacheSimulationRootReportDto>> simulate(

        final long traceId,
        final Set<CacheConfigurationDto> cacheConfigurations,
        final Set<SimulationFlag> simulationFlags

    ) throws RejectedExecutionException {

        final CacheTrace cacheTrace;
        final long beginTimestamp = System.currentTimeMillis();

        try {

            cacheTrace = cacheTraceDao.selectById(traceId);
        }

        catch(final DataAccessException exc) {

            log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());
            return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.UCATEGORIZED, null));
        }

        final Map<String, CacheSimulationRootReportDto> rootReports = new HashMap<>();

        if(cacheTrace != null) {

            final CacheTraceBodyDto trace = jsonMapper.deserialize(cacheTrace.getData(), cacheTraceBodyDtoType);

            for(final CacheConfigurationDto cacheConfiguration : cacheConfigurations) {

                this.simulateSingle(beginTimestamp, cacheConfiguration, trace, simulationFlags, rootReports);
            }
        }

        else {

            return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.NOT_FOUND, null));
        }

        return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.OK, rootReports));
    }

    ///..
    public CacheCommandArguments parseReadWritePrefetch(final String command) {

        final String[] components = command.split(" ");
        return new CacheCommandArguments(Integer.parseInt(components[0].substring(1)), Long.parseLong(components[1], 16));
    }

    ///.
    private Memory buildHierarchy(final MemoryConfigurationDto memoryConfiguration, final EventManager eventManager) {

        if(memoryConfiguration instanceof CacheConfigurationDto cacheConfiguration) {

            return new Cache(

                cacheConfiguration.getAccessTime(),
                cacheConfiguration.getNumSetsExp(),
                cacheConfiguration.getLineSizeExp(),
                cacheConfiguration.getAssociativity(),
                cacheConfiguration.getReplacementPolicyType(),
                this.buildHierarchy(cacheConfiguration.getNextLevelConfiguration(), eventManager),
                eventManager
            ); 
        }

        else {

            return new SystemMemory(

                memoryConfiguration.getAccessTime(),
                eventManager
            );
        }
    }

    ///..
    private void simulateSingle(

        final long beginTimestamp,
        final CacheConfigurationDto cacheConfiguration,
        final CacheTraceBodyDto trace,
        final Set<SimulationFlag> simulationFlags,
        final Map<String, CacheSimulationRootReportDto> rootReports
    ) {

        long cycleCounter = 0;
        long commandCounter = 0;
        final Cache cache = (Cache)this.buildHierarchy(cacheConfiguration, new EventManager());

        for(final String command : trace.getBody()) {

            final CacheCommandType commandType = CacheCommandType.determineType(command);

            if(commandType != CacheCommandType.REPEAT) {

                cycleCounter += this.doSimpleCommand(commandType, command, cache, simulationFlags);
                commandCounter += this.updateCommandCounter(commandType);
            }

            else {

                final String[] commandComponents = command.split("#");
                final int repetitions = Integer.parseInt(commandComponents[1]);
                final List<String> section = trace.getSections().get(commandComponents[2]);

                for(int i = 0; i < repetitions; i++) {

                    final CacheCommandType sectionCommandType = CacheCommandType.determineType(command);

                    for(final String sectionCommand : section) {

                        cycleCounter += this.doSimpleCommand(sectionCommandType, sectionCommand, cache, simulationFlags);
                        commandCounter += this.updateCommandCounter(commandType);
                    }
                }
            }
        }

        final double averageMemoryAccessTime = commandCounter > 0D ? (double)cycleCounter / (double)commandCounter : -1D;

        rootReports.put(cacheConfiguration.getName(), new CacheSimulationRootReportDto(

            SimulationStatus.OK,
            beginTimestamp,
            System.currentTimeMillis(),
            averageMemoryAccessTime,
            cache.getMemorySimulationReportDto()
        ));
    }

    ///..
    private long doSimpleCommand(

        final CacheCommandType commandType,
        final String command,
        final Cache cache,
        final Set<SimulationFlag> simulationFlags
    ) {

        switch(commandType) {

            case READ, WRITE: return this.doReadWritePrefetch(commandType, command, cache);

            case PREFETCH: return simulationFlags.contains(SimulationFlag.IGNORE_PREFETCHES) ? 0L : this.doReadWritePrefetch(commandType, command, cache);

            case FLUSH: return cache.flush();
            case INVALIDATE: return cache.invalidate(Integer.parseInt(command.substring(2)));
            case NOOP: return simulationFlags.contains(SimulationFlag.IGNORE_NOOPS) ? 0L : cache.noop(this.parseNoop(command));

            default: return 0L;
        }
    }

    ///..
    private long doReadWritePrefetch(final CacheCommandType commandType, final String command, final Cache cache) {

        long cycleCounter = 0L;
        final CacheCommandArguments arguments = this.parseReadWritePrefetch(command);

        for(int i = 0; i < arguments.getSize(); i++) {

            final long address = arguments.getAddress() + i;

            switch(commandType) {

                case READ: cycleCounter += cache.read(address); break;
                case WRITE: cycleCounter += cache.write(address); break;

                default: cycleCounter += cache.prefetch(address); break;
            }
        }

        return cycleCounter;
    }

    ///..
    private long updateCommandCounter(final CacheCommandType commandType) {

        return (commandType == CacheCommandType.READ || commandType == CacheCommandType.WRITE) ? 1L : 0L;
    }

    ///..
    private long parseNoop(final String command) {

        if(command.length() == 1) return 1L;
        return Long.parseLong(command.substring(1));
    }

    ///
}
