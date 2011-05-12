package org.ccci.framework.sblio;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.ccci.framework.sblio.annotations.SiebelField;
import org.ccci.framework.sblio.annotations.SiebelFieldValue;
import org.ccci.util.StringUtil;
import org.ccci.util.Util;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * Contains uninteresting conversion logic used by {@link DssDataBean}.
 * 
 * @author Ryan Carlson
 * @author Matt Drees
 *
 */
public class SiebelHelper
{
	
	/**
     * Convert from Java's camelCase to Siebel Styled variable names.
     * For example, convert from "peopleId" to "People Id"
     * 
     * @param camelCase
     * @return
     */
    public static String convertCamelCaseToSpaces(String camelCase)
    {
    	if(Util.isBlank(camelCase)) return camelCase;
    	
        StringBuffer spaces = new StringBuffer();

        for(int i=0; i<camelCase.length(); i++){
            char c = camelCase.charAt(i);
            if(Character.isUpperCase(c)||Character.isDigit(c)) spaces.append(" ");
            spaces.append(c);
        }
        spaces.setCharAt(0, Character.toUpperCase(spaces.charAt(0))); //need to make first letter capital
        return spaces.toString();
    }
    
    /**
     * Convert from SQL-style underscored variable names to Java's camelCase.
     * For example, convert from "PEOPLE_ID" to "peopleId"
     * 
     * Note that this doesn't know about acronyms:
     * CCC_STAFF would be converted into cccStaff and not CCCStaff
     * 
     * @param spaces
     * @return
     */
    public static String convertSpacesToCamelCase(String spaces)
    {
        StringBuffer camelCase = new StringBuffer();
        boolean nextIsSpace = false;
        for(int i=0; i<spaces.length(); i++)
        {
            char c = spaces.charAt(i);
            
            if(c==' ')
            {
                nextIsSpace = true;
            }
            else
            {
                c = nextIsSpace?Character.toUpperCase(c):Character.toLowerCase(c);
                camelCase.append(c);
                nextIsSpace = false;
            }
        }
        return camelCase.toString();
    }
    
    public static String getFieldName(Field field){
    	field.setAccessible(true);
    	
		String fieldName;
		SiebelField sf = field.getAnnotation(SiebelField.class);
		//do some configuration by exception
		if(sf == null){
			fieldName = convertCamelCaseToSpaces(field.getName());
		}else{
			fieldName = sf.field();
		}
		return fieldName;
    }
    


    private static Field getSiebelValueField(Class<?> enumType)
    {
        Field[] fields = enumType.getDeclaredFields();
        for(Field field : fields)
        {
            if (field.isAnnotationPresent(SiebelFieldValue.class))
                return field;
        }
        throw new IllegalArgumentException(String.format(
            "enum type %s does not have a field",
            enumType
        ));
    }

    static Object getFieldValueFromAccessibleField(Object obj, Field field) throws AssertionError
    {
        Object fieldValueObject;
        try
        {
            fieldValueObject = field.get(obj);
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError("field " + field + " was forced to be accessible");
        }
        return fieldValueObject;
    }


    static String convertFieldValueToSiebelValue(Field field, Object fieldValueObject)
    {
        String fieldValue = "";
        Class<?> fieldType = field.getType();

        if(fieldType.equals(Date.class))
        {
            Date fieldValueAsDate = (Date) fieldValueObject;
            fieldValue = SiebelUtil.DATE_TIME_FORMATTER.print(new DateTime(fieldValueAsDate));
        }
        else if (fieldType.equals(DateTime.class))
        {
            DateTime fieldValueAsDateTime = (DateTime) fieldValueObject;
            return SiebelUtil.DATE_TIME_FORMATTER.print(fieldValueAsDateTime);
        }
        else if (fieldType.equals(LocalDate.class))
        {
            LocalDate fieldValueAsLocalDate = (LocalDate) fieldValueObject;
            return SiebelUtil.LOCAL_DATE_FORMATTER.print(fieldValueAsLocalDate);
        }
        else if (Enum.class.isAssignableFrom(fieldType))
        {
            fieldValue = convertEnumToSiebelCode((Enum<?>)fieldValueObject, field);
        }
        else if (fieldType.equals(Boolean.class))
        {
            Boolean fieldValueAsBoolean = (Boolean) fieldValueObject;
            return fieldValueAsBoolean ? "Y" : "N";
        }
        else
        {
            fieldValue = fieldValueObject.toString();
        }
        return fieldValue;
    }

    static String convertEnumToSiebelCode(Enum<?> enumValue, Field columnField)
    {
        Field[] fields = enumValue.getClass().getDeclaredFields();
        for(Field field : fields){
            
            if (field.isAnnotationPresent(SiebelFieldValue.class))
            {
                if (field.getType() != String.class)
                {
                    throw new IllegalArgumentException(String.format(
                        "enum %s's @%s field %s is not of type String, but of type %s",
                        enumValue,
                        SiebelFieldValue.class.getSimpleName(),
                        field,
                        field.getType()
                    ));
                }
                
                field.setAccessible(true);
                return (String) getFieldValueFromAccessibleField(enumValue, field);
            }
        }
        throw new IllegalArgumentException(String.format(
            "enum %s does not have a field annotated @%s, as required if this enum field (%s) is to be mapped to a Siebel column",
            enumValue,
            SiebelFieldValue.class.getSimpleName(),
            columnField
        ));
    }

    static void setFieldValueToAccessibleField(Object obj, Field field, Object convertedValue)
    {
        try
        {
            field.set(obj, convertedValue);
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError("field " + field + " was forced to be accessible");
        }
    }

    
    static Object convertSiebelValueToFieldValue(Class<?> fieldType, String fieldValue)
    {
        if(fieldType.equals(String.class)){
            return fieldValue; //some existing code is depending on blank strings instead of nulls
        }
        else
        {
            if (StringUtil.isBlank(fieldValue))
            {
                return null;
            }
            else if(fieldType.equals(Boolean.class))
            {
                if ("Y".equals(fieldValue)) return Boolean.TRUE;
                if ("N".equals(fieldValue)) return Boolean.FALSE;
                throw new IllegalArgumentException("can't convert to Boolean: " + fieldValue);
            }
            else if(fieldType.equals(Integer.class))
            {
                return new Integer(fieldValue);
            }
            else if(fieldType.equals(Long.class))
            {
                return new Long(fieldValue);
            }
            else if(fieldType.equals(Float.class))
            {
                return new Float(fieldValue);
            }
            else if(fieldType.equals(Date.class))
            {
                return parseDateTime(fieldValue).toDate();
            }
            else if(fieldType.equals(DateTime.class))
            {
                return parseDateTime(fieldValue); //this fails when parsing milliseconds
            }
            else if(fieldType.equals(LocalDate.class))
            {
                return parseLocalDate(fieldValue);
            }
            else if(Enum.class.isAssignableFrom(fieldType) && !Util.isBlank(fieldValue))
            {
                @SuppressWarnings("rawtypes")  //sadly, I think using asSubclass() requires us to use a raw type
                Class<? extends Enum> enumType = fieldType.asSubclass(Enum.class);
                @SuppressWarnings("unchecked") //I can't find a way to do this w/out compiler warnings
                Enum<?> enumValue = convertSiebelCodeToEnum(enumType, fieldValue);
                return enumValue;
            }
            else
            {
                throw new IllegalArgumentException("Can't handle field of type: " + fieldType);
            }
        }
    }

    static private DateTime parseDateTime(String fieldValue)
    {
        return SiebelUtil.DATE_TIME_FORMATTER.parseDateTime(fieldValue);
    }

    static private LocalDate parseLocalDate(String fieldValue)
    {
        return SiebelUtil.LOCAL_DATE_FORMATTER.parseDateTime(fieldValue).toLocalDate();
    }

    static <T extends Enum<T>> T convertSiebelCodeToEnum(Class<T> enumType, String siebelFieldValue)
    {
        EnumSet<T> possibleEnumValues = EnumSet.allOf(enumType);
        
        Field mappingField = getSiebelValueField(enumType);
        mappingField.setAccessible(true);
        
        for (T possibleEnum : possibleEnumValues)
        {
            String code = (String) getFieldValueFromAccessibleField(possibleEnum, mappingField);
            if (code == null)
                throw new IllegalArgumentException(String.format(
                    "siebel value field %s contains null value",
                    mappingField
                ));
            if (code.equals(siebelFieldValue))
                return possibleEnum;
        }
        throw new SblioException(String.format(
            "The value returned by Siebel (%s) does not correspond to any enum of type %s",
            siebelFieldValue,
            enumType
        ));
    }

    /**
     * Note this method will ignore any static fields, and this is not
     * able to be overridden.  
     * @param obj
     * @return
     */
    static List<Field> getAllDeclaredInstanceFields(Object obj)
    {
        Field[] fields;
        ArrayList<Field> fieldList = new ArrayList<Field>();
        Class<?> clazz = obj.getClass();
        
        while(!clazz.equals(Object.class))
        {
            fields = clazz.getDeclaredFields();
            for (Field field : fields)
            {
                if (!Modifier.isStatic(field.getModifiers()))
                {
                    fieldList.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        
        return fieldList;
    }
    
    public static Field getField(Class clazz, String fieldName)
    {
        while(!clazz.getName().equals("java.lang.Object"))
        {
            for(Field f : clazz.getDeclaredFields())
            {
                if(f.getName().equals(fieldName)) return f;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
