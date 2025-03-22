package io.github.clamentos.cachecruncher.business.services;

///
import com.fasterxml.jackson.core.type.TypeReference;

///..
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import io.github.clamentos.cachecruncher.business.simulation.CommandType;

///..
import io.github.clamentos.cachecruncher.business.simulation.cache.Cache;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.CacheTraceDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;

///..
import io.github.clamentos.cachecruncher.web.dtos.CacheConfigurationDto;
import io.github.clamentos.cachecruncher.web.dtos.CacheSimulationReportDto;
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceDto;
import io.github.clamentos.cachecruncher.web.dtos.SimulationArgumentsDto;

///.
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.dao.DataAccessException;

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
    private final DtoValidator dtoValidator;
    private final CacheTraceDao cacheTraceDao;
    private final ObjectMapper objectMapper;

    ///..
    private final TypeReference<CacheTraceBodyDto> cacheTraceBodyDtoType;

    ///
    @Autowired
    public CacheTraceService(DtoValidator dtoValidator, CacheTraceDao cacheTraceDao, ObjectMapper objectMapper) {

        this.dtoValidator = dtoValidator;
        this.cacheTraceDao = cacheTraceDao;
        this.objectMapper = objectMapper;

        cacheTraceBodyDtoType = new TypeReference<CacheTraceBodyDto>(){};
    }

    ///
    @Transactional
    public void create(CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityAlreadyExistsException, IllegalArgumentException, NullPointerException {

        dtoValidator.validateForCreate(cacheTraceDto);

        if(cacheTraceDao.existsByName(cacheTraceDto.getName())) {

            throw new EntityAlreadyExistsException(ErrorFactory.create(

                ErrorCode.CACHE_TRACE_ALREADY_EXISTS,
                "CacheTraceService.create -> Entity already exists",
                cacheTraceDto.getName()
            ));
        }

        CacheTrace entity = new CacheTrace(

            System.currentTimeMillis(),
            cacheTraceDto.getDescription(),
            cacheTraceDto.getName(),
            JsonMapper.serialize(cacheTraceDto.getData(), objectMapper)
        );

        cacheTraceDao.insert(entity);
    }

    ///..
    public CacheTraceDto getById(long id) throws DataAccessException, EntityNotFoundException {

        CacheTrace entity = cacheTraceDao.selectById(id);

        if(entity != null) {

            CacheTraceDto dto = new CacheTraceDto();

            dto.setId(entity.getId());
            dto.setName(entity.getName());
            dto.setDescription(entity.getDescription());
            dto.setCreatedAt(entity.getCreatedAt());
            dto.setUpdatedAt(entity.getUpdatedAt());
            dto.setData(JsonMapper.deserialize(entity.getData(), cacheTraceBodyDtoType, objectMapper));

            return dto;
        }

        throw new EntityNotFoundException(ErrorFactory.create(

            ErrorCode.CACHE_TRACE_NOT_FOUND,
            "CacheTraceService.getById -> Entity does not exist",
            id
        ));
    }

    ///..
    public List<CacheTraceDto> getMinimalByNameLikeAndDates(

        String nameLike,
        long createdAtStart,
        long createdAtEnd,
        long updatedAtStart,
        long updatedAtEnd

    ) throws DataAccessException {

        List<CacheTrace> entities = cacheTraceDao.selectMinimalByNameLikeAndDates(

            "%" + nameLike + "%",
            createdAtStart,
            createdAtEnd,
            updatedAtStart,
            updatedAtEnd
        );

        List<CacheTraceDto> dtos = new ArrayList<>(entities.size());

        for(CacheTrace entity : entities) {

            dtos.add(new CacheTraceDto(

                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                null
            ));
        }

        return dtos;
    }

    ///..
    @Transactional
    public void update(CacheTraceDto cacheTraceDto)
    throws DataAccessException, EntityNotFoundException, IllegalArgumentException, NullPointerException {

        dtoValidator.validateForUpdate(cacheTraceDto);
        CacheTrace entity = cacheTraceDao.selectById(cacheTraceDto.getId());

        if(entity == null) {

            throw new EntityNotFoundException(ErrorFactory.create(

                ErrorCode.CACHE_TRACE_NOT_FOUND,
                "CacheTraceService.update -> Entity does not exist",
                cacheTraceDto.getId()
            ));
        }

        if(cacheTraceDto.getName() != null) {

            if(cacheTraceDao.existsByName(cacheTraceDto.getName())) {

                throw new EntityAlreadyExistsException(ErrorFactory.create(

                    ErrorCode.CACHE_TRACE_ALREADY_EXISTS,
                    "CacheTraceService.update -> Entity already exists",
                    cacheTraceDto.getName()
                ));
            }

            entity.setName(cacheTraceDto.getName());
        }

        if(cacheTraceDto.getDescription() != null) entity.setDescription(cacheTraceDto.getDescription());

        if(cacheTraceDto.getData() != null) {

            entity.setData(JsonMapper.serialize(cacheTraceDto.getData(), objectMapper));
        }

        entity.setUpdatedAt(System.currentTimeMillis());
        cacheTraceDao.update(entity);
    }

    ///..
    public void deleteById(long id) throws DataAccessException {

        cacheTraceDao.delete(id);
    }

    ///..
    public Map<Long, Map<String, CacheSimulationReportDto>> simulate(SimulationArgumentsDto simulationArgumentsDto) {

        // validate...

        Map<Long, Map<String, CacheSimulationReportDto>> combinedReport = new LinkedHashMap<>();

        for(Long traceId : simulationArgumentsDto.getTraceIds()) {

            CacheTrace entity = cacheTraceDao.selectById(traceId);

            if(entity == null) {

                throw new RuntimeException("");
            }

            combinedReport.put(traceId, new LinkedHashMap<>());
            CacheTraceBodyDto trace = JsonMapper.deserialize(entity.getData(), cacheTraceBodyDtoType, objectMapper);

            for(CacheConfigurationDto cacheConfiguration : simulationArgumentsDto.getCacheConfigurations()) {

                Cache cache = buildHierarchy(cacheConfiguration);

                for(String command : trace.getTrace()) {

                    if(CommandType.determineType(command) != CommandType.REPEAT) {
        
                        doSimpleCommandOnCache(command, cache);
                    }
        
                    else {
        
                        String[] splits = command.split("#");
                        int repetitions = Integer.parseInt(splits[1]);
                        List<String> section = trace.getSections().get(splits[2]);
        
                        if(section == null) {
        
                            throw new RuntimeException("");
                        }
        
                        for(int i = 0; i < repetitions; i++) {
        
                            for(String sectionCommand : section) {
        
                                doSimpleCommandOnCache(sectionCommand, cache);
                            }
                        }
                    }
                }

                combinedReport.get(traceId).put(cacheConfiguration.getName(), cache.getSimulationReport());
            }
        }

        return combinedReport;
    }

    ///.
    private Cache buildHierarchy(CacheConfigurationDto cacheConfiguration) {

        Cache nextLevelCache = null;

        if(cacheConfiguration.getNextLevelConfiguration() != null) {

            nextLevelCache = buildHierarchy(cacheConfiguration.getNextLevelConfiguration());
        }

        return new Cache(

            cacheConfiguration.getNumSets(),
            cacheConfiguration.getLineSize(),
            cacheConfiguration.getAssociativity(),
            cacheConfiguration.getReplacementPolicyType(),
            nextLevelCache
        );
    }

    ///..
    private void doSimpleCommandOnCache(String command, Cache cache) {

        switch(CommandType.determineType(command)) {

            case READ: cache.read(Integer.parseInt(command.substring(1))); break;
            case WRITE: cache.write(Integer.parseInt(command.substring(1))); break;

            default: throw new RuntimeException();
        }
    }

    ///
}
