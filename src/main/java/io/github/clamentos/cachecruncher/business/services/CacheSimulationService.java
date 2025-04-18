package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.business.simulation.CacheCommandArguments;
import io.github.clamentos.cachecruncher.business.simulation.CacheCommandType;
import io.github.clamentos.cachecruncher.business.simulation.SimulationFlag;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.Cache;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationReportSummaryDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceBodyDto;

///.
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    public Future<Entry<String, CacheSimulationReportSummaryDto>> simulate(

        Integer ramAccessTime,
        Set<SimulationFlag> simulationFlags,
        CacheConfigurationDto cacheConfiguration,
        CacheTraceBodyDto trace

    ) throws RejectedExecutionException {

        long cycleCounter = 0;
        long commandCounter = 0;
        Cache cache = this.buildHierarchy(cacheConfiguration, ramAccessTime);
        long beginTimestamp = System.currentTimeMillis();

        for(String command : trace.getTrace()) {

            CacheCommandType commandType = CacheCommandType.determineType(command);

            if(commandType != CacheCommandType.REPEAT) {

                cycleCounter += this.doSimpleCommandOnCache(commandType, command, cache);
                commandCounter = this.updateCommandCounter(commandCounter, commandType);
            }

            else {

                String[] splits = command.split("#");
                int repetitions = Integer.parseInt(splits[1]);
                List<String> section = trace.getSections().get(splits[2]);

                for(int i = 0; i < repetitions; i++) {

                    CacheCommandType sectionCommandType = CacheCommandType.determineType(command);

                    for(String sectionCommand : section) {

                        cycleCounter += this.doSimpleCommandOnCache(sectionCommandType, sectionCommand, cache);
                        commandCounter = this.updateCommandCounter(commandCounter, commandType);
                    }
                }
            }
        }

        CacheSimulationReportSummaryDto summary = new CacheSimulationReportSummaryDto(

            beginTimestamp,
            System.currentTimeMillis(),
            commandCounter > 0 ? (double) cycleCounter / (double) commandCounter : -1,
            cache.getSimulationReport()
        );

        return CompletableFuture.completedFuture(Map.entry(cacheConfiguration.getName(), summary));
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

        if(commandType == CacheCommandType.READ) {

            long cycles = 0;
            CacheCommandArguments arguments = this.parseSimpleCommand(command);

            for(int i = 0; i < arguments.getSize(); i++) {

                cycles += cache.read(arguments.getAddress() + i);
            }

            return cycles;
        }

        if(commandType == CacheCommandType.WRITE) {

            long cycles = 0;
            CacheCommandArguments arguments = this.parseSimpleCommand(command);

            for(int i = 0; i < arguments.getSize(); i++) {

                cycles += cache.write(arguments.getAddress() + i);
            }

            return cycles;
        }

        // PREFETCH

        if(commandType == CacheCommandType.FLUSH) return cache.flush();
        if(commandType == CacheCommandType.INVALIDATE) return cache.invalidate(Integer.parseInt(command.substring(2)));

        return cache.noop();
    }

    ///..
    private long updateCommandCounter(long commandCounter, CacheCommandType commandType) {

        if(commandType == CacheCommandType.READ || commandType == CacheCommandType.WRITE) {

            return commandCounter + 1;
        }

        return commandCounter;
    }

    ///..
    private CacheCommandArguments parseSimpleCommand(String command) {

        String[] splits = command.split(" ");

        return new CacheCommandArguments(

            Integer.parseInt(splits[0].substring(1)),
            Long.parseLong(splits[1], 16)
        );
    }

    ///
}
