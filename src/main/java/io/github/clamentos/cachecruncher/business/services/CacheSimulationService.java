package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.business.simulation.CommandType;
import io.github.clamentos.cachecruncher.business.simulation.SimulationFlag;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.Cache;

///..
import io.github.clamentos.cachecruncher.web.dtos.CacheConfigurationDto;
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.SimulationReportSummaryDto;

///.
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

///..
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Entry<String, SimulationReportSummaryDto>> simulate(

        Integer ramAccessTime,
        Set<SimulationFlag> simulationFlags,
        CacheConfigurationDto cacheConfiguration,
        CacheTraceBodyDto trace
    ) {

        long cycleCounter = 0;
        long commandCounter = 0;
        Cache cache = this.buildHierarchy(cacheConfiguration, ramAccessTime);
        long beginTimestamp = System.currentTimeMillis();

        for(String command : trace.getTrace()) {

            if(CommandType.determineType(command) != CommandType.REPEAT) {

                cycleCounter += this.doSimpleCommandOnCache(command, cache);
                commandCounter++;
            }

            else {

                String[] splits = command.split("#");
                int repetitions = Integer.parseInt(splits[1]);
                List<String> section = trace.getSections().get(splits[2]);

                for(int i = 0; i < repetitions; i++) {

                    for(String sectionCommand : section) {

                        cycleCounter += this.doSimpleCommandOnCache(sectionCommand, cache);
                        commandCounter++;
                    }
                }
            }
        }

        SimulationReportSummaryDto summary = new SimulationReportSummaryDto(

            beginTimestamp,
            System.currentTimeMillis(),
            commandCounter > 0 ? (double) cycleCounter / (double) commandCounter : 1,
            cache.getSimulationReport()
        );

        return CompletableFuture.completedFuture(Map.entry(cacheConfiguration.getName(), summary));
    }

    ///
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
    private long doSimpleCommandOnCache(String command, Cache cache) {

        switch(CommandType.determineType(command)) {

            case READ: return cache.read(Integer.parseInt(command.substring(1)));
            case WRITE: return cache.write(Integer.parseInt(command.substring(1)));
            case PREFETCH: return cache.prefetch(Integer.parseInt(command.substring(1)));
            case FLUSH: return cache.flush();

            default: return cache.noop();
        }
    }

    ///
}
