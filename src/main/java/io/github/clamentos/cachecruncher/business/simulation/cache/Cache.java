package io.github.clamentos.cachecruncher.business.simulation.cache;

///
import io.github.clamentos.cachecruncher.business.simulation.replacement.LruReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.NoOpReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.RandomReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationReportDto;
import io.github.clamentos.cachecruncher.web.dtos.report.MemorySimulationReportDto;

///.
import java.util.ArrayList;
import java.util.List;

///.
import lombok.Getter;

///
public class Cache {

    ///
    private final int ramAccessTime;
    private final int accessTime;
    private final int lineSizeExp;
    private final ReplacementPolicy replacementPolicy;
    private final List<List<CacheLine>> sets;
    private final Cache nextLevelCache;

    ///..
    @Getter
    private final CacheSimulationReportDto simulationReportDto;

    ///..
    private final long indexMask;
    private final int tagShiftAmount;

    ///
    public Cache(

        int ramAccessTime,
        int accessTime,
        int numSetsExp,
        int lineSizeExp,
        int associativity,
        ReplacementPolicyType replacementPolicyType,
        Cache nextLevelCache
    ) {

        this.ramAccessTime = ramAccessTime;
        this.accessTime = accessTime;
        this.lineSizeExp = lineSizeExp;

        int numSetsPow = numSetsExp > 0 ? 2 << (numSetsExp - 1) : 1;
        sets = new ArrayList<>(numSetsPow);

        for(int i = 0; i < numSetsPow; i++) {

            List<CacheLine> ways = new ArrayList<>(associativity);

            for(int j = 0; j < associativity; j++) {

                ways.add(new CacheLine());
            }

            sets.add(ways);
        }

        switch(replacementPolicyType) {

            case NOOP: replacementPolicy = new NoOpReplacementPolicy(); break;
            case RANDOM: replacementPolicy = new RandomReplacementPolicy(associativity); break;
            case LRU: replacementPolicy = new LruReplacementPolicy(associativity, numSetsPow); break;

            default: replacementPolicy = new NoOpReplacementPolicy(); break;
        }

        this.nextLevelCache = nextLevelCache;
        simulationReportDto = new CacheSimulationReportDto();

        simulationReportDto.setNextLevelReport(

            nextLevelCache == null ?
            new MemorySimulationReportDto() :
            nextLevelCache.getSimulationReportDto()
        );

        long indexMaskTmp = 0;

        for(int i = 0; i < numSetsExp; i++) {

            indexMaskTmp |= (1 << i);
        }

        indexMask = indexMaskTmp;
        tagShiftAmount = numSetsExp + lineSizeExp;
    }

    ///
    public long read(long address) {

        long cycleCounter = 0;
        int index = this.extractIndex(address);

        if(!lookup(address, index, false)) {

            simulationReportDto.setReadMisses(simulationReportDto.getReadMisses() + 1);

            if(nextLevelCache != null) {

                cycleCounter += nextLevelCache.read(address);
            }

            else {

                MemorySimulationReportDto ramReport = simulationReportDto.getNextLevelReport();

                ramReport.setReadRequests(ramReport.getReadRequests() + 1);
                cycleCounter += ramAccessTime;
            }

            int victimWay = replacementPolicy.getVictim(index);
            CacheLine victim = sets.get(index).get(victimWay);

            if(victim.isValid() && victim.isDirty()) {

                if(nextLevelCache != null) {

                    cycleCounter += nextLevelCache.write(address);
                }

                else {

                    MemorySimulationReportDto ramReport = simulationReportDto.getNextLevelReport();

                    ramReport.setWriteRequests(ramReport.getWriteRequests() + 1);
                    cycleCounter += ramAccessTime;
                }
            }

            else {

                victim.setValid(true);
            }

            victim.setDirty(false);
            victim.setTag(address >> tagShiftAmount);
            replacementPolicy.update(index, victimWay);
        }

        return cycleCounter;
    }

    ///..
    public long write(long address) {

        long cycleCounter = accessTime;
        int index = this.extractIndex(address);

        if(!lookup(address, index, true)) {

            simulationReportDto.setWriteMisses(simulationReportDto.getWriteMisses() + 1);

            if(nextLevelCache != null) {

                cycleCounter += nextLevelCache.read(address);
            }

            else {

                MemorySimulationReportDto ramReport = simulationReportDto.getNextLevelReport();

                ramReport.setReadRequests(ramReport.getReadRequests() + 1);
                cycleCounter += ramAccessTime;
            }

            int victimWay = replacementPolicy.getVictim(index);
            CacheLine victim = sets.get(index).get(victimWay);

            if(victim.isValid() && victim.isDirty()) {

                if(nextLevelCache != null) {

                    cycleCounter += nextLevelCache.write(address);
                }

                else {

                    MemorySimulationReportDto ramReport = simulationReportDto.getNextLevelReport();

                    ramReport.setWriteRequests(ramReport.getWriteRequests() + 1);
                    cycleCounter += ramAccessTime;
                }
            }

            else {

                victim.setValid(true);
            }

            victim.setDirty(true);
            victim.setTag(address >> tagShiftAmount);
            replacementPolicy.update(index, victimWay);
        }

        return cycleCounter;
    }

    ///..
    public long prefetch(long address) {

        // ...
        return 1;
    }

    ///..
    public long noop(long num) {

        return num;
    }

    ///..
    public long flush() {

        long cycleCounter = 0;

        for(List<CacheLine> lines : sets) {

            for(CacheLine line : lines) {

                line.setValid(false);
                cycleCounter += ramAccessTime;
            }
        }

        if(nextLevelCache != null) {

            cycleCounter += nextLevelCache.flush();
        }

        return cycleCounter;
    }

    ///..
    public long invalidate(long address) {

        long cycleCounter = 0;
        long tagFromAddress = address >> tagShiftAmount;
        int index = this.extractIndex(address);
        List<CacheLine> ways = sets.get(index);

        for(CacheLine way : ways) {

            if(way.getTag() == tagFromAddress) {

                way.setValid(false);
                cycleCounter++;
            }
        }

        if(nextLevelCache != null) {

            cycleCounter += nextLevelCache.invalidate(address);
        }

        return cycleCounter;
    }

    ///.
    private boolean lookup(long address, int index, boolean isWriteMode) {

        if(isWriteMode) simulationReportDto.setWriteRequests(simulationReportDto.getWriteRequests() + 1);
        else simulationReportDto.setReadRequests(simulationReportDto.getReadRequests() + 1);

        long tagFromAddress = address >> tagShiftAmount;
        List<CacheLine> cacheSet = sets.get(index);

        for(int i = 0; i < cacheSet.size(); i++) {

            CacheLine cacheLine = cacheSet.get(i);

            if(cacheLine.isValid() && cacheLine.getTag() == tagFromAddress) {

                if(isWriteMode) cacheLine.setDirty(true);
                replacementPolicy.update(index, i);

                return true;
            }
        }

        return false;
    }

    ///..
    private int extractIndex(long address) {

        long shifted = address >> lineSizeExp;
        return (int) (shifted & indexMask);
    }

    ///
}
