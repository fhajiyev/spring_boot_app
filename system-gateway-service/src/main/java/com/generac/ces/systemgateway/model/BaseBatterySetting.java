package com.generac.ces.systemgateway.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseBatterySetting implements Serializable {

    @JsonProperty("socMax")
    @JsonAlias("soc_max")
    private Double socMax;

    @JsonProperty("socMin")
    @JsonAlias("soc_min")
    private Double socMin;

    @JsonProperty("socRsvMax")
    @JsonAlias("soc_rsv_max")
    private Double socRsvMax;

    @JsonProperty("socRsvMin")
    @JsonAlias("soc_rsv_min")
    private Double socRsvMin;

    @JsonProperty("aChaMax")
    @JsonAlias("a_cha_max")
    private Double aChaMax;

    @JsonProperty("aDisChaMax")
    @JsonAlias("a_dis_cha_max")
    private Double aDisChaMax;
}
