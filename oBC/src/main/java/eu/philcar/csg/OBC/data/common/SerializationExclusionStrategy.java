package eu.philcar.csg.OBC.data.common;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class SerializationExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(ExcludeSerialization.class) != null;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return clazz.getAnnotation(ExcludeSerialization.class) != null;
    }

}
