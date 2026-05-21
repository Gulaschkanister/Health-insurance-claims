package de.gkvtransmitter.converter;

public class TypeConverter {

    private TypeConverter() {
    }

    /**
     * Konvertiert einen String in einfache Zieltypen für UI-bezogene
     * Eingabeverarbeitung.
     */
    public static Object convertStringToSimpleType(Class<?> clazz, String stringType) {
        if (Boolean.class == clazz)
            return Boolean.valueOf(stringType);

        if (Integer.class == clazz)
            return Integer.valueOf(stringType);

        if (Float.class == clazz)
            return Float.valueOf(stringType);
        if (Double.class == clazz)
            return Double.valueOf(stringType);
        return stringType;
    }
}
