package io.github.clamentos.cachecruncher.business.simulation.cache;

///
import io.github.clamentos.cachecruncher.business.simulation.replacement.LruReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.NoOpReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.RandomReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

///..
import io.github.clamentos.cachecruncher.web.dtos.CacheSimulationReportDto;
import io.github.clamentos.cachecruncher.web.dtos.SimulationReportDto;

///.
import java.util.ArrayList;
import java.util.List;

///
public final class Cache {

    ///
    private final int lineSize;
    private final int numSets;
    private final ReplacementPolicy replacementPolicy;
    private final List<List<CacheLine>> cacheLines;
    private final Cache nextLevelCache;

    ///..
    private final CacheSimulationReportDto simulationReportDto;

    ///..
    private final int indexMask;

    ///
    public Cache(int numSets, int lineSize, int associativity, ReplacementPolicyType replacementPolicyType, Cache nextLevelCache) {

        this.lineSize = lineSize;
        this.numSets = numSets;

        int numSetsPow = numSets > 0 ? 2 << (numSets - 1) : 1;
        cacheLines = new ArrayList<>(numSetsPow);

        for(int i = 0; i < numSetsPow; i++) {

            List<CacheLine> ways = new ArrayList<>(associativity);

            for(int j = 0; j < associativity; j++) {

                ways.add(new CacheLine());
            }

            cacheLines.add(ways);
        }

        switch(replacementPolicyType) {

            case NOOP: replacementPolicy = new NoOpReplacementPolicy(); break;
            case RANDOM: replacementPolicy = new RandomReplacementPolicy(associativity); break;
            case LRU: replacementPolicy = new LruReplacementPolicy(associativity, numSets); break;

            default: replacementPolicy = null;
        }

        this.nextLevelCache = nextLevelCache;
        simulationReportDto = new CacheSimulationReportDto();

        simulationReportDto.setNextLevelCacheReport(

            nextLevelCache == null ?
            new SimulationReportDto() :
            nextLevelCache.getSimulationReport()
        );

        int indexMaskTmp = 0;

        for(int i = 0; i < numSets; i++) {

            indexMaskTmp |= (1 << i);
        }

        indexMask = indexMaskTmp;
    }

    ///
    public void read(int address) {

        simulationReportDto.setReadRequests(simulationReportDto.getReadRequests() + 1);

        boolean hit = false;
        int index = this.extractIndex(address);
        List<CacheLine> cacheSet = cacheLines.get(index);

        for(int i = 0; i < cacheSet.size(); i++) {

            int tagFromAddress = this.extractTag(address);
            CacheLine cacheLine = cacheSet.get(i);

            if(cacheLine.isValid() && cacheLine.getTag() == tagFromAddress) {

                hit = true;
                replacementPolicy.update(index, i);

                break;
            }
        }

        if(!hit) {

            simulationReportDto.setReadMisses(simulationReportDto.getReadMisses() + 1);

            if(nextLevelCache != null) {

                nextLevelCache.read(address);
            }

            else {

                SimulationReportDto ramReport = simulationReportDto.getNextLevelCacheReport();
                ramReport.setReadRequests(ramReport.getReadRequests() + 1);
            }

            int victimWay = replacementPolicy.getVictim(index);
            CacheLine victim = cacheSet.get(victimWay);

            if(victim.isValid() && victim.isDirty()) {

                if(nextLevelCache != null) {

                    nextLevelCache.write(address);
                }

                else {

                    SimulationReportDto ramReport = simulationReportDto.getNextLevelCacheReport();
                    ramReport.setWriteRequests(ramReport.getWriteRequests() + 1);
                }
            }

            else {

                victim.setValid(true);
            }

            victim.setDirty(false);
            victim.setTag(this.extractTag(address));
            replacementPolicy.update(index, victimWay);
        }
    }

    ///..
    public void write(int address) {

        simulationReportDto.setWriteRequests(simulationReportDto.getWriteRequests() + 1);

        boolean hit = false;
        int index = this.extractIndex(address);
        List<CacheLine> cacheSet = cacheLines.get(index);

        for(int i = 0; i < cacheSet.size(); i++) {

            int tagFromAddress = this.extractTag(address);
            CacheLine cacheLine = cacheSet.get(i);

            if(cacheLine.isValid() && cacheLine.getTag() == tagFromAddress) {

                hit = true;
                cacheLine.setDirty(true);
                replacementPolicy.update(index, i);

                break;
            }
        }

        if(!hit) {

            simulationReportDto.setWriteMisses(simulationReportDto.getWriteMisses() + 1);

            if(nextLevelCache != null) {

                nextLevelCache.read(address);
            }

            else {

                SimulationReportDto ramReport = simulationReportDto.getNextLevelCacheReport();
                ramReport.setReadRequests(ramReport.getReadRequests() + 1);
            }

            int victimWay = replacementPolicy.getVictim(index);
            CacheLine victim = cacheSet.get(victimWay);

            if(victim.isValid() && victim.isDirty()) {

                if(nextLevelCache != null) {

                    nextLevelCache.write(address);
                }

                else {

                    SimulationReportDto ramReport = simulationReportDto.getNextLevelCacheReport();
                    ramReport.setWriteRequests(ramReport.getWriteRequests() + 1);
                }
            }

            else {

                victim.setValid(true);
            }

            victim.setDirty(true);
            victim.setTag(this.extractTag(address));
            replacementPolicy.update(index, victimWay);
        }
    }

    ///..
    public CacheSimulationReportDto getSimulationReport() {

        return simulationReportDto;
    }

    ///.
    private int extractIndex(int address) {

        int tmp = address >> lineSize;
        return tmp & indexMask;
    }

    ///..
    private int extractTag(int address) {

        return address >> (numSets + lineSize);
    }

    ///
}
