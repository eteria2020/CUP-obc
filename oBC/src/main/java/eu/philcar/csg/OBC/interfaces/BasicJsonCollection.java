package eu.philcar.csg.OBC.interfaces;

/**
 * Created by Fulvio on 06/10/2017.
 */

public interface BasicJsonCollection {

    boolean add(BasicJson unit);

    BasicJson remove(int index);

    String toJson();

    BasicJson get(int index);

    boolean contains(BasicJson unit);

    BasicJson find(BasicJson unit);


}
