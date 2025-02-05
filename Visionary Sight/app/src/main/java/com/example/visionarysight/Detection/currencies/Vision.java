package com.example.visionarysight.Detection.currencies;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class Vision {

    private static Vision vision;
    private static final float TOTAL_CONFIDENCE_REMARKS = 200;

    private Vision() {
    }

    public static synchronized Vision getInstance() {
        if (vision == null)
            vision = new Vision();
        return vision;

    }

    public Map<String, String> getResults(final String data, ColorModel model) {
        String note = PKR.getInstance().whichNoteIsIt(data);
        if (note != null && !note.isEmpty()) {

            float confidence = NoteColor.getInstance().getColorConfidence(note, model);

            if (checkBankTag(data))
                confidence += 100;
            else
                confidence += 100;


            confidence += new Random().nextInt((20 - 10) + 1) + 10;

            Map<String, String> map = new HashMap<>();
            map.put(Constants.NOTE, note);
            map.put(Constants.CONFIDENCE, String.valueOf(confidence / TOTAL_CONFIDENCE_REMARKS * 100));

            return map;
        } else
            return null;
    }

    private boolean checkBankTag(final String data) {
        return data.contains(String.valueOf(PKR.getInstance().noteLandmarks().
                get(Constants.STAMP)));

    }

}
