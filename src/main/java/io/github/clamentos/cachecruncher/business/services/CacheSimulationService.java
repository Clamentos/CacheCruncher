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
    public CacheSimulationService(CacheTraceDao cacheTraceDao, JsonMapper jsonMapper) {

        this.cacheTraceDao = cacheTraceDao;
        this.jsonMapper = jsonMapper;

        cacheTraceBodyDtoType = new TypeReference<>(){};
    }

    ///
    @Async(value = "simulationsExecutor")
    public Future<SimulationReport<CacheSimulationRootReportDto>> simulate(

        long traceId,
        Set<CacheConfigurationDto> cacheConfigurations,
        Set<SimulationFlag> simulationFlags

    ) throws RejectedExecutionException {

        CacheTrace cacheTrace;
        long beginTimestamp = System.currentTimeMillis();

        try {

            cacheTrace = cacheTraceDao.selectById(traceId);
        }

        catch(DataAccessException exc) {

            log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());
            return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.UCATEGORIZED, null));
        }

        Map<String, CacheSimulationRootReportDto> rootReports = new HashMap<>();

        if(cacheTrace != null) {

            CacheTraceBodyDto trace = jsonMapper.deserialize(cacheTrace.getData(), cacheTraceBodyDtoType);

            for(CacheConfigurationDto cacheConfiguration : cacheConfigurations) {

                long cycleCounter = 0;
                long commandCounter = 0;
                Cache cache = (Cache)this.buildHierarchy(cacheConfiguration, new EventManager());

                for(String command : trace.getBody()) {

                    CacheCommandType commandType = CacheCommandType.determineType(command);

                    if(commandType != CacheCommandType.REPEAT) {

                        cycleCounter += this.doSimpleCommand(commandType, command, cache, simulationFlags);
                        commandCounter += this.updateCommandCounter(commandType);
                    }

                    else {

                        String[] commandComponents = command.split("#");
                        int repetitions = Integer.parseInt(commandComponents[1]);
                        List<String> section = trace.getSections().get(commandComponents[2]);

                        for(int i = 0; i < repetitions; i++) {

                            CacheCommandType sectionCommandType = CacheCommandType.determineType(command);

                            for(String sectionCommand : section) {

                                cycleCounter += this.doSimpleCommand(sectionCommandType, sectionCommand, cache, simulationFlags);
                                commandCounter += this.updateCommandCounter(commandType);
                            }
                        }
                    }
                }

                double averageMemoryAccessTime = commandCounter > 0 ? (double)cycleCounter / (double)commandCounter : -1;

                rootReports.put(cacheConfiguration.getName(), new CacheSimulationRootReportDto(

                    SimulationStatus.OK,
                    beginTimestamp,
                    System.currentTimeMillis(),
                    averageMemoryAccessTime,
                    cache.getMemorySimulationReportDto()
                ));
            }
        }

        else {

            return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.NOT_FOUND, null));
        }

        return CompletableFuture.completedFuture(new SimulationReport<>(SimulationStatus.OK, rootReports));
    } 

    ///.
    private Memory buildHierarchy(MemoryConfigurationDto memoryConfiguration, EventManager eventManager) {

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
    private long doSimpleCommand(CacheCommandType commandType, String command, Cache cache, Set<SimulationFlag> simulationFlags) {

        switch(commandType) {

            case READ, WRITE: return this.doReadWritePrefetch(commandType, command, cache);

            case PREFETCH: return simulationFlags.contains(SimulationFlag.IGNORE_PREFETCHES) ? 0 : this.doReadWritePrefetch(commandType, command, cache);

            case FLUSH: return cache.flush();
            case INVALIDATE: return cache.invalidate(Integer.parseInt(command.substring(2)));
            case NOOP: return simulationFlags.contains(SimulationFlag.IGNORE_NOOPS) ? 0 : cache.noop(this.parseNoop(command));

            default: return 0;
        }
    }

    ///..
    private long doReadWritePrefetch(CacheCommandType commandType, String command, Cache cache) {

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

    ///..
    private long parseNoop(String command) {

        if(command.length() == 1) return 1;
        return Long.parseLong(command.substring(1));
    }

    ///
}
