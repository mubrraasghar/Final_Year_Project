package com.example.visionarysight.Detection.currencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class PKR {

    private static PKR pkr;
    private static final Map<String, String> notes = new HashMap<>();

    private static final ArrayList<String> keys = new ArrayList<>();

    private PKR() {

        notes.put("10", "Ten Rupees");
        notes.put("TEN", "Ten Rupees");

        notes.put("20", "Twenty Rupees");
        notes.put("TWENTY", "Twenty Rupees");

        notes.put("50", "Fifty Rupees");
        notes.put("FIFTY", "Fifty Rupees");

        notes.put("100", "One Hundred Rupees");
        notes.put("ONE HUNDRED", "One Hundred Rupees");

        notes.put("500", "Five Hundred Rupees");
        notes.put("FIVE HUNDRED", "Five Hundred Rupees");

        notes.put("1000", "One Thousand Rupees");
        notes.put("ONE THOUSAND", "On Thousand Rupees");

        notes.put("5000", "Five Thousand Rupees");
        notes.put("FIVE THOUSAND", "Five Thousand Rupees");


        keys.add("10");
        keys.add("TEN");

        keys.add("20");
        keys.add("TWENTY");

        keys.add("50");
        keys.add("FIFTY");

        keys.add("100");
        keys.add("ONE HUNDRED");

        keys.add("500");
        keys.add("FIVE HUNDRED");

        keys.add("1000");
        keys.add("ONE THOUSAND");

        keys.add("5000");
        keys.add("FIVE THOUSAND");
    }


    public static synchronized PKR getInstance() {
        if (pkr == null)
            pkr = new PKR();
        return pkr;
    }

    public String liveNoteDetection(final String data){
        String r="";
        for(String key: keys){
            if(data.contains(key))
                r=notes.get(key);
        }
        return r;
    }


    public String whichNoteIsIt(final String data) {

        String[] splited = data.split("\\s+");
        String r = "";

        for (String key : keys) {

            for(String value: splited){
               if(value.equalsIgnoreCase(key))
                   return notes.get(key);
            }
        }
        return r;
    }


    public Map<String, Object> noteLandmarks() {
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.STAMP,"STATE BANK OF PAKISTAN");
        map.put(Constants.SERIAL,7);
        map.put(Constants.CHARACTERS,3);
        return map;
    }
}
