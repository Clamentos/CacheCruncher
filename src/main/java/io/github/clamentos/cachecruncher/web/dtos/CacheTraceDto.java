package io.github.clamentos.cachecruncher.web.dtos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

///
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

///
public final class CacheTraceDto {

    ///
    private Long id;
    private String name;
    private String description;
    private Long createdAt;
    private Long updatedAt;
    private CacheTraceBodyDto data;

    ///
}
