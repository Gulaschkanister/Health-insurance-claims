package de.gkvtransmitter.domain.converter;

public class TypeConverter {
    // TODO: wird voraussichtlich nicht mehr verwendet dachte ursprünglich es wäre
    // praktisch für den Spinner createProzess
    public static Object convertStringToSimpleType(Class clazz, String stringType) {
        if (Boolean.class == clazz)
            return Boolean.parseBoolean(stringType);

        if (Integer.class == clazz)
            return Integer.parseInt(stringType);

        if (Float.class == clazz)
            return Float.parseFloat(stringType);
        if (Double.class == clazz)
            return Double.parseDouble(stringType);
        return stringType;
    }
}
