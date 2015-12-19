package utils;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by rjewing on 2/18/14.
 */
public class QueryParams {

    /**
     * return QueryParams minus any error and optionally return_to params
     * @param paramMap
     * @return
     */
    public String getQueryParams(Map paramMap, Boolean removeReturnTo) {
        StringBuilder sb = new StringBuilder();
        sb.append("&");
        for (Iterator iterator = paramMap.entrySet().iterator(); iterator.hasNext();)  {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (!entry.getKey().equals("error") && !(removeReturnTo && entry.getKey().equals("return_to"))) {
                sb.append(entry.getKey());
                sb.append("=");
                String [] vals = (String []) paramMap.get(entry.getKey());
                sb.append(vals[0]);
                sb.append("&");
            }
        }
        if (sb.indexOf("&") != -1) {
            sb.deleteCharAt(sb.lastIndexOf("&"));
        }
        System.out.print(sb.toString());
        return sb.toString();
    }
}
