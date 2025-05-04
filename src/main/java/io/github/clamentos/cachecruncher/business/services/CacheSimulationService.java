package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.business.simulation.CacheCommandArguments;
import io.github.clamentos.cachecruncher.business.simulation.CacheCommandType;
import io.github.clamentos.cachecruncher.business.simulation.SimulationFlag;
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.Cache;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationRootReportDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceBodyDto;

///.
import java.util.List;
import java.util.Set;

///..
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

///.
import org.springframework.scheduling.annotation.Async;

///..
import org.springframework.stereotype.Service;

///
@Service

///
public class CacheSimulationService {

    ///
    @Async(value = "simulationsExecutor")
    public Future<CacheSimulationRootReportDto> simulate(

        int ramAccessTime,
        Set<SimulationFlag> simulationFlags,
        CacheConfigurationDto cacheConfiguration,
        CacheTraceBodyDto trace

    ) throws RejectedExecutionException {

        long cycleCounter = 0;
        long commandCounter = 0;
        long beginTimestamp = System.currentTimeMillis();
        Cache cache = this.buildHierarchy(cacheConfiguration, ramAccessTime);

        for(String command : trace.getBody()) {

            CacheCommandType commandType = CacheCommandType.determineType(command);

            if(commandType != CacheCommandType.REPEAT) {

                cycleCounter += this.doSimpleCommandOnCache(commandType, command, cache);
                commandCounter += this.updateCommandCounter(commandType);
            }

            else {

                String[] commandComponents = command.split("#");
                int repetitions = Integer.parseInt(commandComponents[1]);
                List<String> section = trace.getSections().get(commandComponents[2]);

                for(int i = 0; i < repetitions; i++) {

                    CacheCommandType sectionCommandType = CacheCommandType.determineType(command);

                    for(String sectionCommand : section) {

                        cycleCounter += this.doSimpleCommandOnCache(sectionCommandType, sectionCommand, cache);
                        commandCounter += this.updateCommandCounter(commandType);
                    }
                }
            }
        }

        if(simulationFlags.contains(SimulationFlag.FLUSH_ON_TERMINATION)) {

            cache.flush();
        }

        double averageMemoryAccessTime = commandCounter > 0 ? (double)cycleCounter / (double)commandCounter : -1;

        CacheSimulationRootReportDto summary = new CacheSimulationRootReportDto(

            SimulationStatus.OK,
            beginTimestamp,
            System.currentTimeMillis(),
            averageMemoryAccessTime,
            cache.getSimulationReport()
        );

        return CompletableFuture.completedFuture(summary);
    }

    ///.
    private Cache buildHierarchy(CacheConfigurationDto cacheConfiguration, int ramAccessTime) {

        Cache nextLevelCache = null;

        if(cacheConfiguration.getNextLevelConfiguration() != null) {

            nextLevelCache = this.buildHierarchy(cacheConfiguration.getNextLevelConfiguration(), ramAccessTime);
        }

        return new Cache(

            ramAccessTime,
            cacheConfiguration.getAccessTime(),
            cacheConfiguration.getNumSetsExp(),
            cacheConfiguration.getLineSizeExp(),
            cacheConfiguration.getAssociativity(),
            cacheConfiguration.getReplacementPolicyType(),
            nextLevelCache
        );
    }

    ///..
    private long doSimpleCommandOnCache(CacheCommandType commandType, String command, Cache cache) {

        switch(commandType) {

            case READ, WRITE, PREFETCH:

                long cycles = 0;
                CacheCommandArguments arguments = this.parseReadWritePrefetch(command);

                for(int i = 0; i < arguments.getSize(); i++) {

                    long address = arguments.getAddress() + i;

                    switch(commandType) {

                        case READ: cycles += cache.read(address); break;
                        case WRITE: cycles += cache.write(address); break;

                        default: cycles += cache.prefetch(address); break;
                    }
                }

            return cycles;

            case FLUSH: return cache.flush();
            case INVALIDATE: return cache.invalidate(Integer.parseInt(command.substring(2)));

            default: return cache.noop();
        }
    }

    ///..
    private CacheCommandArguments parseReadWritePrefetch(String command) {

        String[] components = command.split(" ");

        return new CacheCommandArguments(

            Integer.parseInt(components[0].substring(1)),
            Long.parseLong(components[1], 16)
        );
    }

    ///..
    private long updateCommandCounter(CacheCommandType commandType) {

        return (commandType == CacheCommandType.READ || commandType == CacheCommandType.WRITE) ? 1 : 0;
    }

    ///
}
