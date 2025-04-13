package ru.sosgps.wayrecall.billing.account.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.BasicDBList;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAggregateTranslate;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;
import scala.Function0;
import scala.runtime.AbstractFunction0;


public class AccountDataSetEvent implements Serializable,WayrecallAxonEvent {
    private final ObjectId accountId;
    private final HashMap<String, Object> data;
    public String toHRString(){
        if(this.data==null) return "";
        StringBuilder sb = new StringBuilder();
        HashMap<String, Object> eventdata = new HashMap<>();
        ArrayList<Map<String, Object>> contracts = null;
        eventdata.putAll(this.data);
        String contractCount = (String) eventdata.remove("contractCount");
        if (contractCount != null && Integer.parseInt(contractCount) > 0) {
            ArrayList contractslist = (ArrayList) eventdata.remove("contracts");
            if(contractslist!=null) contracts=contractslist;
        }
        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            sb.append(WayrecallAggregateTranslate.AccountTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                };
            }));
            sb.append(":\'");
            sb.append(item.getValue());
            sb.append("\'; ");
        }
        if(contractCount!=null) sb.append("число контрактов:'" + contractCount + "'; ");
        if (contracts != null && !contracts.isEmpty()) {
            sb.append("контракты:[");
            for (final Map<String, Object> contract : contracts) {
                for(final Map.Entry<String, Object> citem : contract.entrySet()) {
                    sb.append(WayrecallAggregateTranslate.AccountTranslate().getOrElse(citem.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return citem.getKey();
                        };
                    }));
                    sb.append(":\'");
                    sb.append(citem.getValue());
                    sb.append("\'; ");
                }
            }
            sb.append("]; ");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> toHRTable() {
        HashMap<String, Object> result = new HashMap<>();
        HashMap<String, Object> eventdata = new HashMap<>();
        ArrayList<Map<String, Object>> contracts = null;
        eventdata.putAll(this.data);
        String contractCount = (String) eventdata.remove("contractCount");
        if (contractCount != null && Integer.parseInt(contractCount) > 0) {
            ArrayList contractslist = (ArrayList) eventdata.remove("contracts");
            if (contractslist != null) contracts = contractslist;
        }
        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            result.put(WayrecallAggregateTranslate.AccountTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                }
            }), item.getValue());
        }
        if (contractCount != null) result.put("Число контрактов", contractCount);
        ArrayList<Map<String, Object>> contractsTranslated = new ArrayList<>();
        if (contracts != null && !contracts.isEmpty()) {
            for (final Map<String, Object> contract : contracts) {
                Map<String, Object> contractTranslated = new HashMap<>();
                for (final Map.Entry<String, Object> citem : contract.entrySet()) {
                    contractTranslated.put(WayrecallAggregateTranslate.AccountTranslate().getOrElse(citem.getKey(), new AbstractFunction0<String>() {
                        @Override
                        public String apply() {
                            return citem.getKey();
                        }

                        ;
                    }), citem.getValue());
                }
                contractsTranslated.add(contractTranslated);
            }
            result.put("Контракты", contractsTranslated);
        }
        return result;

    }

    public AccountDataSetEvent(ObjectId accountId, HashMap<String, Object> data) {
        this.accountId = accountId;        
        this.data=data;
    }

    public ObjectId getAccountId() {
        return accountId;
    }

    public HashMap<String, Object> getData() {
        return data;
    }
    
    
    
}
