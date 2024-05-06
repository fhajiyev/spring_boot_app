package com.generac.ces.systemgateway.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Actor {
    /* Priority should be in sync with default_actors in beacon (https://github.com/neurio/system_controller/blob/main/system_controller/system_controller.py) */
    UNKNOWN("", -1),
    HOMEOWNER("home_owner", 10),
    STORMPREP("storm_prep", 20),
    ENBALA("enbala", 30),
    PWRFLEET("pwrfleet", 50);

    public final String actorId;
    public final int priority;

    public static Actor fromActorId(String actorId) {
        return Arrays.stream(Actor.values())
                .filter(actor -> actor.getActorId().equals(actorId))
                .findFirst()
                .orElse(UNKNOWN);
    }

    @JsonCreator
    public static Actor fromActorString(String actor) {
        for (Actor currActor : Actor.values()) {
            if (currActor.toString().equalsIgnoreCase(actor)) {
                return currActor;
            }
        }
        return UNKNOWN;
    }

    public static List<String> convertToStringList(List<Actor> actors) {
        return actors.stream().map(Actor::getActorId).collect(Collectors.toList());
    }
}
