package io.github.clamentos.cachecruncher.business.simulation.cache;

///
import io.github.clamentos.cachecruncher.business.simulation.event.EventManager;

///..
import io.github.clamentos.cachecruncher.business.simulation.replacement.FifoReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.LruReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.NoOpReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.RandomReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicy;
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.CacheSimulationReportDto;

///.
import java.util.ArrayList;
import java.util.List;

///.
import lombok.Getter;

///
public final class Cache implements Memory {

    ///
    private final List<List<CacheLine>> ways;

    ///..
    @Getter
    private final long accessTime;

    private final int lineSizeExp;
    private final ReplacementPolicy replacementPolicy;

    ///..
    private final Memory nextLevelCache;
    private final EventManager eventManager;

    ///..
    private final CacheSimulationReportDto cacheSimulationReportDto;

    ///..
    private final long indexMask;
    private final int tagShiftAmount;
    private final long ramAccessTime;

    ///
    public Cache(

        final long accessTime,
        final int numSetsExp,
        final int lineSizeExp,
        final int associativity,
        final ReplacementPolicyType replacementPolicyType,
        final Memory nextLevelCache,
        final EventManager eventManager
    ) {

        final int numSetsPow = numSetsExp > 0 ? 2 << (numSetsExp - 1) : 1;
        ways = new ArrayList<>(numSetsPow);

        for(int i = 0; i < associativity; i++) {

            final List<CacheLine> lines = new ArrayList<>(numSetsPow);
            for(int j = 0; j < numSetsPow; j++) lines.add(new CacheLine());

            ways.add(lines);
        }

        this.accessTime = accessTime;
        this.lineSizeExp = lineSizeExp;

        switch(replacementPolicyType) {

            case RANDOM: replacementPolicy = new RandomReplacementPolicy(associativity); break;
            case LRU: replacementPolicy = new LruReplacementPolicy(associativity, numSetsPow); break;
            case FIFO: replacementPolicy = new FifoReplacementPolicy(associativity, numSetsPow); break;

            default: replacementPolicy = new NoOpReplacementPolicy(); break;
        }

        this.nextLevelCache = nextLevelCache;
        this.eventManager = eventManager;

        cacheSimulationReportDto = new CacheSimulationReportDto();
        cacheSimulationReportDto.setNextLevelReport(nextLevelCache.getMemorySimulationReportDto());

        long indexMaskTmp = 0;
        for(int i = 0; i < numSetsExp; i++) indexMaskTmp |= (1 << i);

        indexMask = indexMaskTmp;
        tagShiftAmount = numSetsExp + lineSizeExp;
        ramAccessTime = this.resolveRamAccessTime();
    }

    ///
    @Override
    public long advance(final long cycles) {

        return eventManager.advance(cycles);
    }

    ///..
    @Override
    public long read(final long address) {

        return this.readOrWrite(address, false);
    }

    ///..
    @Override
    public long write(final long address) {

        return this.readOrWrite(address, true);
    }

    ///..
    @Override
    public CacheSimulationReportDto getMemorySimulationReportDto() {

        return cacheSimulationReportDto;
    }

    ///..
    // currently prefetches only 1 byte...
    // makes little sense rn...
    public long prefetch(final long address) {

        long atCycle = eventManager.getCurrentCycle() + ramAccessTime;
        eventManager.addEvent(atCycle, () -> this.write(address));

        return this.advance(1);
    }

    ///..
    public long noop(final long num) {

        return this.advance(num);
    }

    ///..
    public long flush() {

        long cycleCounter = 0;

        for(final List<CacheLine> way : ways) {

            for(final CacheLine line : way) {

                line.setValid(false);
                cycleCounter += this.advance(ramAccessTime);
            }
        }

        if(nextLevelCache instanceof Cache nextCache) cycleCounter += nextCache.flush();
        return cycleCounter;
    }

    ///..
    public long invalidate(final long address) {

        long cycleCounter = 0;
        final long tagFromAddress = address >> tagShiftAmount;
        final int index = this.extractIndex(address);
        final List<CacheLine> lines = ways.get(index);

        for(final CacheLine line : lines) {

            if(line.getTag() == tagFromAddress) {

                line.setValid(false);
                cycleCounter++;
            }
        }

        if(nextLevelCache instanceof Cache nextCache) cycleCounter += nextCache.invalidate(address);
        return cycleCounter;
    }

    ///.
    private long readOrWrite(final long address, final boolean isWriteMode) {

        long cycleCounter = this.advance(accessTime);
        final int index = this.extractIndex(address);

        if(!this.isHit(address, index, isWriteMode)) {

            if(isWriteMode) cacheSimulationReportDto.setWriteMisses(cacheSimulationReportDto.getWriteMisses() + 1);
            else cacheSimulationReportDto.setReadMisses(cacheSimulationReportDto.getReadMisses() + 1);

            cycleCounter += nextLevelCache.read(address);

            final int victimWay = replacementPolicy.getVictim(index);
            final CacheLine victim = ways.get(victimWay).get(index);

            if(victim.isValid() && victim.isDirty()) cycleCounter += nextLevelCache.write(address);
            else victim.setValid(true);

            victim.setDirty(isWriteMode);
            victim.setTag(address >> tagShiftAmount);

            replacementPolicy.update(index, victimWay, false);
        }

        return cycleCounter;
    }

    ///..
    private int extractIndex(final long address) {

        final long shifted = address >> lineSizeExp;
        return (int) (shifted & indexMask);
    }

    ///..
    private boolean isHit(final long address, final int index, final boolean isWriteMode) {

        if(isWriteMode) cacheSimulationReportDto.setWriteRequests(cacheSimulationReportDto.getWriteRequests() + 1);
        else cacheSimulationReportDto.setReadRequests(cacheSimulationReportDto.getReadRequests() + 1);

        final long tag = address >> tagShiftAmount;

        for(int i = 0; i < ways.size(); i++) {

            final CacheLine cacheLine = ways.get(i).get(index);

            if(cacheLine.isValid() && cacheLine.getTag() == tag) {

                replacementPolicy.update(index, i, true);
                if(isWriteMode) cacheLine.setDirty(true);

                return true;
            }
        }

        return false;
    }

    ///..
    private long resolveRamAccessTime() {

        if(nextLevelCache instanceof final Cache nextCache) return nextCache.resolveRamAccessTime();
        return nextLevelCache.getAccessTime();
    }

    ///
}
