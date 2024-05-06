package com.generac.ces.systemgateway.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Quota {
    private int startingQuotaSeconds;
    private int remainingQuotaSeconds;
}
