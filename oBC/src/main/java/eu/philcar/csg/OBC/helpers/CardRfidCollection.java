package eu.philcar.csg.OBC.helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import eu.philcar.csg.OBC.interfaces.BasicJson;
import eu.philcar.csg.OBC.interfaces.BasicJsonCollection;

/**
 * Created by Fulvio on 06/10/2017.
 */

public class CardRfidCollection implements BasicJsonCollection {
    private DLog dlog = new DLog(this.getClass());

    private ArrayList<CardRfid> cards;

    public CardRfidCollection() {
        cards = new ArrayList<>();
    }

    public static CardRfidCollection decodeFromJson(String json) {
        CardRfidCollection result = new CardRfidCollection();

        JSONArray ja;
        try {
            if (!json.equalsIgnoreCase("") && !json.equalsIgnoreCase("{}")) {
                ja = new JSONArray(json);

                if (ja.length() > 0) {
                    for (int i = 0; i < ja.length(); i++) {
                        JSONObject jo = ja.optJSONObject(i);
                        if (jo != null) {
                            result.add(CardRfid.decodeFromJson(jo.toString()));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            DLog.E("Parsing OpenDoorsCardsSetup JSON", e);
        }
        return result;

    }

    @Override
    public String toJson() {

        JSONArray ja = new JSONArray();
        for (CardRfid card : cards) {
            ja.put(card.toJsonObject());
        }
        return ja.toString();
    }

    @Override
    public boolean add(BasicJson card) {
        if (card instanceof CardRfid && card.isValid()) {
            cards.add((CardRfid) card);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public BasicJson remove(int index) {
        return cards.remove(index);
    }

    @Override
    public BasicJson get(int index) {
        return cards.get(index);
    }

    @Override
    public boolean contains(BasicJson unit) {
        if (unit instanceof CardRfid) {

            return cards.contains(unit);
        } else {
            return false;
        }
    }

    @Override
    public BasicJson find(BasicJson unit) {
        try {
            if (unit instanceof CardRfid) {
                int index = cards.indexOf(unit);

                return index >= 0 ? cards.get(index) : null;
            } else {
                return null;
            }
        } catch (Exception e) {
            dlog.e("exception while finding card", e);
            return null;
        }
    }
}
