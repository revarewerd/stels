
package ru.sosgps.wayrecall.billing.tariff.events;

import java.io.Serializable;
import java.util.*;

import com.mongodb.BasicDBList;
import org.bson.types.ObjectId;
import scala.runtime.AbstractFunction0;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;
import ru.sosgps.wayrecall.core.WayrecallAggregateTranslate;

import static ru.sosgps.wayrecall.utils.ScalaConverters.asJavaList;

public class TariffPlanDataSetEvent implements Serializable,WayrecallAxonEvent {
 private final ObjectId tariffId;
    private final HashMap<String,Object> data;
    public String toHRString(){
        if(this.data==null) return "";
        StringBuilder sb = new StringBuilder();
        HashMap<String, Object> eventdata = new HashMap<>();
        eventdata.putAll(this.data);
        Object ap = (Object) eventdata.remove("abonentPrice");
        Object addap= (Object)  eventdata.remove("additionalAbonentPrice");
        Object sp = (Object)  eventdata.remove("servicePrice");
        Object hp = (Object)  eventdata.remove("hardwarePrice");
        java.util.List<Map<String,Object>> abonentPrice= null;
        if(ap!=null) {
            if (ap.getClass().equals(BasicDBList.class)) {
                abonentPrice = (List<Map<String, Object>>) ap;
            } else abonentPrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) ap);
        }

        java.util.List<Map<String,Object>> additionalAbonentPrice = null;
        if(addap!=null) {
            if (addap.getClass().equals(BasicDBList.class)) {
                additionalAbonentPrice = (List<Map<String, Object>>) addap;
            } else additionalAbonentPrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) addap);
        }

        java.util.List<Map<String,Object>> servicePrice= null;
        if(sp!=null) {
            if (sp.getClass().equals(BasicDBList.class)) {
                servicePrice = (List<Map<String, Object>>) sp;
            } else servicePrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) sp);
        }

        java.util.List<Map<String,Object>> hardwarePrice= null;
        if(hp!=null) {
            if (hp.getClass().equals(BasicDBList.class)) {
                hardwarePrice = (List<Map<String, Object>>) hp;
            } else hardwarePrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) hp);
        }

        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            sb.append(WayrecallAggregateTranslate.TariffPlanTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                };
            }));
            sb.append(":\'");
            Object value=item.getValue();
            if(item.getValue().getClass().equals(scala.Some.class))
                value=((scala.Some) item.getValue()).get();
            sb.append(value);
            sb.append("\'; ");
        }
        if (abonentPrice != null && ! abonentPrice.isEmpty()) {
            sb.append("тарифы абонентские:[");
            for (final Map<String, Object> prices : abonentPrice) {
                for (final Map.Entry<String, Object> price : prices.entrySet()) {
                    sb.append(WayrecallAggregateTranslate.TariffPlanTranslate().getOrElse(price.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return price.getKey();
                        }

                        ;
                    }));
                    sb.append(":\'");
                    sb.append(price.getValue());
                    sb.append("\'; ");
                }
            }
            sb.append("]; ");
        }
        if (additionalAbonentPrice != null && ! additionalAbonentPrice.isEmpty()) {
            sb.append("ежемесячные услуги:[");
            for (final Map<String, Object> prices : additionalAbonentPrice) {
                for (final Map.Entry<String, Object> price : prices.entrySet()) {
                    sb.append(WayrecallAggregateTranslate.TariffPlanTranslate().getOrElse(price.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return price.getKey();
                        };
                    }));
                    sb.append(":\'");
                    sb.append(price.getValue());
                    sb.append("\'; ");
                }
            }
            sb.append("]; ");
        }
        if (servicePrice != null && ! servicePrice.isEmpty()) {
            sb.append("единовременные услуги:[");
            for (final Map<String, Object> prices : servicePrice) {
                for (final Map.Entry<String, Object> price : prices.entrySet()) {
                    sb.append(WayrecallAggregateTranslate.TariffPlanTranslate().getOrElse(price.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return price.getKey();
                        }

                        ;
                    }));
                    sb.append(":\'");
                    sb.append(price.getValue());
                    sb.append("\'; ");
                }
            }
            sb.append("]; ");
        }
        if (hardwarePrice != null && ! hardwarePrice.isEmpty()) {
            sb.append("железо:[");
            for (final Map<String, Object> prices : hardwarePrice) {
                for (final Map.Entry<String, Object> price : prices.entrySet()) {
                    sb.append(WayrecallAggregateTranslate.TariffPlanTranslate().getOrElse(price.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return price.getKey();
                        }

                        ;
                    }));
                    sb.append(":\'");
                    sb.append(price.getValue());
                    sb.append("\'; ");
                }
            }
            sb.append("]; ");
        }
        return sb.toString();
    }
    public TariffPlanDataSetEvent(ObjectId tariffId, HashMap<String, Object> data) {
        this.tariffId = tariffId;
        this.data = data;
    }

    @Override
    public Map<String, Object> toHRTable() {
        HashMap<String, Object> result = new HashMap<>();
        if(this.data==null) return result;
        //  StringBuilder sb = new StringBuilder();
        HashMap<String, Object> eventdata = new HashMap<>();
        eventdata.putAll(this.data);
        Object ap = (Object) eventdata.remove("abonentPrice");
        Object addap= (Object)  eventdata.remove("additionalAbonentPrice");
        Object sp = (Object)  eventdata.remove("servicePrice");
        Object hp = (Object)  eventdata.remove("hardwarePrice");
        List<Map<String,Object>> abonentPrice= null;
        if(ap!=null) {
            if (ap.getClass().equals(BasicDBList.class)) {
                abonentPrice = (List<Map<String, Object>>) ap;
            } else abonentPrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) ap);
        }

        List<Map<String,Object>> additionalAbonentPrice = null;
        if(addap!=null) {
            if (addap.getClass().equals(BasicDBList.class)) {
                additionalAbonentPrice = (List<Map<String, Object>>) addap;
            } else additionalAbonentPrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) addap);
        }

        List<Map<String,Object>> servicePrice= null;
        if(sp!=null) {
            if (sp.getClass().equals(BasicDBList.class)) {
                servicePrice = (List<Map<String, Object>>) sp;
            } else servicePrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) sp);
        }

        List<Map<String,Object>> hardwarePrice= null;
        if(hp!=null) {
            if (hp.getClass().equals(BasicDBList.class)) {
                hardwarePrice = (List<Map<String, Object>>) hp;
            } else hardwarePrice = asJavaList((scala.collection.immutable.List<Map<String, Object>>) hp);
        }

        Map<String, List<Map<String,Object>>> tariffData = new LinkedHashMap<>();
        tariffData.put("Тарифы абонентские", abonentPrice);
        tariffData.put("ежемесячные услуги", additionalAbonentPrice);
        tariffData.put("Единовременные услуги", servicePrice);
        tariffData.put("Железо", hardwarePrice);

        for(Map.Entry<String, List<Map<String,Object>>> e : tariffData.entrySet()) {
            List<Map<String, Object>> aPrice = e.getValue();
            if(aPrice != null && !aPrice.isEmpty()) {
                HashMap<String, Map<String, Object>> pricesTranslated = new HashMap<>();
                for (final Map<String, Object> prices : aPrice) {
                    Map<String, Object> priceTranslated = new HashMap<>();
                    String name = "";
                    for (final Map.Entry<String, Object> price : prices.entrySet()) {
                        if(price.getKey().equals("name"))
                            name = Objects.toString(price.getValue());
                        else {
                            String tkey = WayrecallAggregateTranslate.TariffPlanTranslate().getOrElse(price.getKey(), new AbstractFunction0<String>() {
                                @Override
                                public String apply() {
                                    return price.getKey();
                                }

                                ;
                            });
                            priceTranslated.put(tkey, price.getValue());
                        }
                    }
                    pricesTranslated.put(name, priceTranslated);
                }
                result.put(e.getKey(),pricesTranslated);
            }

        }
        return result;
    }
    public ObjectId getTariffId() {
        return tariffId;
    }

    public HashMap<String, Object> getData() {
        return data;
    }
}
