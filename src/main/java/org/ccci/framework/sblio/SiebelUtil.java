package org.ccci.framework.sblio;

import com.google.common.base.Strings;
import org.ccci.framework.sblio.annotations.ChildBusinessCompField;
import org.ccci.framework.sblio.annotations.Key;
import org.ccci.framework.sblio.annotations.MvgField;
import org.ccci.framework.sblio.annotations.ReadOnly;
import org.ccci.framework.sblio.annotations.Transient;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.reflect.Field;

public class SiebelUtil
{

    private static final String SIEBEL_DATE_TIME_PATTERN = "MM/dd/yyyy HH:mm:ss";

    private static final String SIEBEL_DATE_PATTERN = "MM/dd/yyyy";

    /** prints and parses dates using the format that Siebel uses for dates without a time component: {@code MM/dd/yyyy} */
    public static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormat.forPattern(SIEBEL_DATE_PATTERN);
    
    /** prints and parses dates using the format that Siebel uses for dates with a time component: {@code MM/dd/yyyy hh:mm:ss} */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(SIEBEL_DATE_TIME_PATTERN);

    static boolean isTransientField(Field field)
    {
    	return field.getAnnotation(Transient.class) != null;
    }

    static boolean isManyToManyField(Field field)
    {
        MvgField md = field.getAnnotation(MvgField.class);
    	return (md!=null && md.manyToMany());
    }
    
    static boolean isMvgField(Field field)
    {
        return field.getAnnotation(MvgField.class) != null;
    }

    static boolean isChildBusinessCompField(Field field)
    {
        return field.getAnnotation(ChildBusinessCompField.class) != null;
    }

    static boolean isKeyField(Field field)
    {
    	return field.getAnnotation(Key.class) != null;
    }

    static boolean isReadOnlyField(Field field)
    {
    	ReadOnly ro = field.getAnnotation(ReadOnly.class);
    	
    	return ro!=null;
    }

    static String determineSiebelFieldNameForMvgField(Object parentObj, String fieldName)
    {
        String siebelFieldName = fieldName;
        Field field = SiebelHelper.getField(parentObj.getClass(), fieldName);
        if(field==null) return fieldName;
        MvgField fieldMetadata = field.getAnnotation(MvgField.class);
        if(fieldMetadata!=null && !Strings.isNullOrEmpty(fieldMetadata.name())) siebelFieldName = fieldMetadata.name();
        return siebelFieldName;
    }

    static String determineSiebelFieldNameForChildBusinessCompField(Object parentObj, String fieldName)
    {
        String siebelFieldName = fieldName;
        Field field = SiebelHelper.getField(parentObj.getClass(), fieldName);
        if(field==null) return fieldName;
        ChildBusinessCompField fieldMetadata = field.getAnnotation(ChildBusinessCompField.class);
        if(fieldMetadata!=null && !Strings.isNullOrEmpty(fieldMetadata.name())) siebelFieldName = fieldMetadata.name();
        return siebelFieldName;
    }
}
