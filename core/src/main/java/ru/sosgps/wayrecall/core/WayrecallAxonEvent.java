package ru.sosgps.wayrecall.core;


import scala.Option;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface WayrecallAxonEvent {
     String toHRString();
    // TODO убрать default после того, как метод будет определен для всех событий
     default Map<String, Object> toHRTable() { // Не знаю, что значит HR
         return new HashMap<>();
     }

    default Option<String> getInitialName() { return Option.apply(null); }
}
