package ru.izotov.currencyrate;

import org.json.JSONException;
import org.json.JSONObject;

public class Currency {
    private String id;
    private int numCode;
    private String charCode;
    private int nominal;
    private String name;
    private double value;

    public String getId() {
        return id;
    }

    public int getNumCode() {
        return numCode;
    }

    public String getCharCode() {
        return charCode;
    }

    public int getNominal() {
        return nominal;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public double getPrevious() {
        return previous;
    }

    double previous;

    Currency(JSONObject jsonObject) {
        try {
            id = jsonObject.getString("ID");
            numCode = jsonObject.getInt("NumCode");
            charCode = jsonObject.getString("CharCode");
            nominal = jsonObject.getInt("Nominal");
            name = jsonObject.getString("Name");
            value = jsonObject.getDouble("Value");
            previous = jsonObject.getDouble("Previous");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
