package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.business.simulation.SimulationFlag;
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.Cache;
import io.github.clamentos.cachecruncher.business.simulation.cache.Memory;
import io.github.clamentos.cachecruncher.business.simulation.cache.SystemMemory;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandType;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeA;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeB;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeC;

///..
import io.github.clamentos.cachecruncher.business.simulation.event.EventManager;

///..
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.SimulationReport;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;
import io.github.clamentos.cachecruncher.web.dtos.simulation.MemoryConfigurationDto;

///.
import java.util.HashMap;
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

    ///
    @Autowired
    public CacheSimulationService(final CacheTraceDao cacheTraceDao) {

        this.cacheTraceDao = cacheTraceDao;
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

        catch(final DatabaseException exc) {

            log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());
            return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.UCATEGORIZED, null));
        }

        final Map<String, CacheSimulationRootReportDto> rootReports = new HashMap<>();

        if(cacheTrace != null) {

            final CacheTraceBody trace = cacheTrace.getTrace();

            for(final CacheConfigurationDto cacheConfiguration : cacheConfigurations) {

                this.simulateSingle(beginTimestamp, cacheConfiguration, trace, simulationFlags, rootReports);
            }
        }

        else {

            return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.NOT_FOUND, null));
        }

        return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.OK, rootReports));
    }

    ///.
    private Memory buildHierarchy(final MemoryConfigurationDto memoryConfiguration, final EventManager eventManager) {

        if(memoryConfiguration instanceof final CacheConfigurationDto cacheConfiguration) {

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

            return new SystemMemory(memoryConfiguration.getAccessTime(), eventManager);
        }
    }

    ///..
    private void simulateSingle(

        final long beginTimestamp,
        final CacheConfigurationDto cacheConfiguration,
        final CacheTraceBody trace,
        final Set<SimulationFlag> simulationFlags,
        final Map<String, CacheSimulationRootReportDto> rootReports
    ) {

        final Cache cache = (Cache)this.buildHierarchy(cacheConfiguration, new EventManager());
        long cycleCounter = 0;
        long commandCounter = 0;

        for(final CacheCommand command : trace.getBody()) {

            final CacheCommandType commandType = command.getType();

            if(commandType != CacheCommandType.REPEAT) {

                cycleCounter += this.doSimpleCommand(command, cache, simulationFlags);
                commandCounter += this.updateCommandCounter(commandType);
            }

            else {

                final CacheCommandTypeC commandTypeC = (CacheCommandTypeC)command;

                for(int i = 0; i < commandTypeC.getRepeats(); i++) {

                    for(final CacheCommand sectionCommand : trace.getSections().get(commandTypeC.getSectionName())) {

                        cycleCounter += this.doSimpleCommand(sectionCommand, cache, simulationFlags);
                        commandCounter += this.updateCommandCounter(sectionCommand.getType());
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

        final CacheCommand command,
        final Cache cache,
        final Set<SimulationFlag> simulationFlags
    ) {

        final CacheCommandType commandType = command.getType();
        long cycleCounter = 0L;

        switch(commandType) {

            case READ, WRITE: cycleCounter = this.doReadWritePrefetch((CacheCommandTypeB)command, cache); break;

            case PREFETCH:

                if(!simulationFlags.contains(SimulationFlag.IGNORE_PREFETCHES)) {

                    cycleCounter = this.doReadWritePrefetch((CacheCommandTypeB)command, cache);
                }

            break;

            case FLUSH: cycleCounter = cache.flush(); break;
            case INVALIDATE: cycleCounter = cache.invalidate(((CacheCommandTypeB)command).getValue()); break;

            case NOOP:
            
                if(!simulationFlags.contains(SimulationFlag.IGNORE_NOOPS)) {

                    cycleCounter = cache.noop(((CacheCommandTypeA)command).getSize());
                }

            break;

            default: cycleCounter = 0L; break;
        }

        return cycleCounter;
    }

    ///..
    private long doReadWritePrefetch(final CacheCommandTypeB command, final Cache cache) {

        long cycleCounter = 0L;

        for(int i = 0; i < command.getSize(); i++) {

            final long address = command.getValue() + i;

            switch(command.getType()) {

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

    ///
}
