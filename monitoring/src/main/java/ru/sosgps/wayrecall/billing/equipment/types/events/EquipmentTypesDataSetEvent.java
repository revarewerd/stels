
package ru.sosgps.wayrecall.billing.equipment.types.events;

import java.io.Serializable;
import java.util.*;

import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAggregateTranslate;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;
import scala.runtime.AbstractFunction0;

public class EquipmentTypesDataSetEvent implements Serializable,WayrecallAxonEvent {
 
    private final ObjectId eqTypeId; 
    private final HashMap<String, Object> data;
    public String toHRString(){
        if(this.data==null) return "";
        StringBuilder sb = new StringBuilder();
        HashMap<String, Object> eventdata = new HashMap<>();
        eventdata.putAll(this.data);
        ArrayList<Map<String,Object>> sensorparams = (ArrayList<Map<String,Object>>) eventdata.remove("sensorparams");
        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            sb.append(WayrecallAggregateTranslate.EquipmentTypeTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                };
            }));
            sb.append(":\'");
            sb.append(item.getValue());
            sb.append("\'; ");
        }
        if (sensorparams != null && ! sensorparams.isEmpty()) {
            sb.append("параметры датчиков:[");
            for (final Map<String, Object> sensorparam : sensorparams) {
                for (final Map.Entry<String, Object> citem : sensorparam.entrySet()) {
                    sb.append(WayrecallAggregateTranslate.EquipmentTypeTranslate().getOrElse(citem.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return citem.getKey();
                        }

                        ;
                    }));
                    sb.append(":\'");
                    sb.append(citem.getValue());
                    sb.append("\', ");
                }
            }
            sb.append("]; ");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> toHRTable() {
        if(this.data==null) return new HashMap<>();
        HashMap<String, Object> eventdata = new HashMap<>();
        eventdata.putAll(this.data);
        ArrayList<Map<String,Object>> sensorparams = (ArrayList<Map<String,Object>>) eventdata.remove("sensorparams");
        HashMap<String, Object> result = new HashMap<>();
        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            String key = WayrecallAggregateTranslate.EquipmentTypeTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                }
            });
            result.put(key,item.getValue());
        }
        if (sensorparams != null && ! sensorparams.isEmpty()) {

            List<Map<String,Object>> sensorParamsTranslated = new ArrayList<>();
            for (final Map<String, Object> sensorparam : sensorparams) {
                String name = "";
                Map<String, Object> sensorParamTranslated = new HashMap<>();
                for (final Map.Entry<String, Object> citem : sensorparam.entrySet()) {
                    sensorParamTranslated.put(WayrecallAggregateTranslate.EquipmentTypeTranslate().getOrElse(citem.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return citem.getKey();
                        }
                    }), citem.getValue());

                }
                sensorParamsTranslated.add(sensorParamTranslated);

            }
            result.put("параметры датчиков", sensorParamsTranslated);
        }
        return result;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public ObjectId getEqTypeId() {
        return eqTypeId;
    }
    public EquipmentTypesDataSetEvent(ObjectId eqTypeId,HashMap<String, Object> data) {
        this.eqTypeId = eqTypeId;     
        this.data=data;
    }
}
