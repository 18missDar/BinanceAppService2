package com.demo;

import lombok.SneakyThrows;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.FieldProperty;
import org.yaml.snakeyaml.introspector.MethodProperty;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Supplier;

public class YamlReaderCreator {

    private YamlReaderCreator() {
    }

    public static Yaml create(Supplier<Constructor> constructor) {
        return new Yaml(constructor.get());
    }

    public static Yaml create() {
        return create(toLowerCamelCaseConstructor());
    }

    /**
     * convert dash separated property names to lower camel case
     * e.g. property-name -> propertyName
     */
    private static Supplier<Constructor> toLowerCamelCaseConstructor() {
        return () -> {
            Constructor constructor = new Constructor();
            PropertyUtils propertyUtils = new PropertyUtils() {
                @Override
                public Property getProperty(Class<?> type, String name) {
                    String camelCase = StringUtils.capitalize(name);
                    camelCase = camelCase.replace("-", "");
                    camelCase = StringUtils.uncapitalize(camelCase);
                    return super.getProperty(type, camelCase);
                }
            };
            propertyUtils.setSkipMissingProperties(true);
            constructor.setPropertyUtils(propertyUtils);
            return constructor;
        };
    }


    private static MethodProperty getMethodPropertyProxy(Property property, Map<String, Object> defProperties) throws IllegalAccessException {
        Field fieldProperty = ReflectionUtils.findField(property.getClass(), "property");
        boolean isAccessible = fieldProperty.isAccessible();
        fieldProperty.setAccessible(true);
        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) fieldProperty.get(property);
        if (isAccessible)
            fieldProperty.setAccessible(true);
        return new MethodProperty(propertyDescriptor) {
            @Override
            public void set(Object object, Object value) throws Exception {
                try {
                    super.set(object, value);
                } catch (Exception e) {
                    Object defValue = defProperties.get(this.getName());
                    if (defValue != null) {
                        super.set(object, defValue);
                        return;
                    }
                    throw e;
                }
            }
        };
    }

    private static FieldProperty getFieldPropertyProxy(Property property, Map<String, Object> defProperties) throws IllegalAccessException {
        Field fieldProperty = ReflectionUtils.findField(property.getClass(), "field");
        boolean isAccessible = fieldProperty.isAccessible();
        fieldProperty.setAccessible(true);
        Field field = (Field) fieldProperty.get(property);
        if (isAccessible)
            fieldProperty.setAccessible(true);

        return new FieldProperty(field) {
            @Override
            public void set(Object object, Object value) throws Exception {
                try {
                    super.set(object, value);
                } catch (Exception e) {
                    Object defValue = defProperties.get(this.getName());
                    if (defValue != null) {
                        super.set(object, defValue);
                        return;
                    }
                    throw e;
                }
            }
        };
    }
}