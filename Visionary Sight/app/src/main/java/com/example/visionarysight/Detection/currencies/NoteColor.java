package com.example.visionarysight.Detection.currencies;

import java.util.HashMap;
import java.util.Map;

public final class NoteColor {
    private static NoteColor color;
    private Map<String, ColorModel> map;

    private NoteColor() {

        map = new HashMap<>();
        ColorModel ten = new ColorModel();
        ten.setBlueMax(145);
        ten.setBlueMin(90);
        ten.setGreenMax(255);
        ten.setGreenMin(140);
        ten.setRedMax(150);
        ten.setRedMin(90);

        map.put("Ten Rupees", ten);

        ColorModel t20 = new ColorModel();
        t20.setGreenMin(115);
        t20.setGreenMax(190);
        t20.setRedMin(140);
        t20.setRedMax(225);
        t20.setBlueMin(40);
        t20.setBlueMax(150);
        map.put("Twenty Rupees", t20);



        ColorModel fifty = new ColorModel();
        fifty.setBlueMax(155);
        fifty.setBlueMin(90);
        fifty.setRedMax(155);
        fifty.setRedMin(125);
        fifty.setGreenMax(200);
        fifty.setGreenMin(120);
        map.put("Fifty Rupees", fifty);

        ColorModel hundred = new ColorModel();
        hundred.setRedMin(170);
        hundred.setRedMax(220);
        hundred.setGreenMin(90);
        hundred.setGreenMax(130);
        hundred.setBlueMin(100);
        hundred.setBlueMax(135);
        map.put("One Hundred Rupees", hundred);


        ColorModel f00 = new ColorModel();
        f00.setBlueMax(155);
        f00.setBlueMin(100);
        f00.setRedMax(200);
        f00.setRedMin(110);
        f00.setGreenMax(195);
        f00.setGreenMin(115);
        map.put("Five Hundred Rupees", f00);


        ColorModel _1000 = new ColorModel();
        _1000.setGreenMin(50);
        _1000.setGreenMax(150);
        _1000.setRedMin(50);
        _1000.setRedMax(160);
        _1000.setBlueMin(180);
        _1000.setBlueMax(225);
        map.put("One Thousand Rupees", _1000);


        ColorModel _5000 = new ColorModel();
        _5000.setBlueMax(219);
        _5000.setBlueMin(150);
        _5000.setRedMax(255);
        _5000.setRedMin(180);
        _5000.setGreenMax(90);
        _5000.setGreenMin(10);
        map.put("Five Thousand Rupees", _5000);

    }

    static synchronized NoteColor getInstance() {
        if (color == null)
            color = new NoteColor();
        return color;
    }

    float getColorConfidence(final String note, ColorModel model) {
        ColorModel color = map.get(note);
        float total = 30;
        float confidence = 3;

        if (color != null) {
            boolean case1 = model.getRedMax() > color.getRedMin() || model.getRedMax() <= color.getRedMax();

            boolean case2 = model.getGreenMax() > color.getGreenMin() || model.getGreenMax() <= color.getGreenMax();

            boolean case3 = model.getBlueMax() > color.getBlueMin() || model.getBlueMax() <= color.getBlueMax();

            if (case1)
                confidence += 9;
            else if (case2)
                confidence += 9;
            else if (case3)
                confidence += 9;

            return (confidence / total * 100);
        } else
            return 1;
    }

}
