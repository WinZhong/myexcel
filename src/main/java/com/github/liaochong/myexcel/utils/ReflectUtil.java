/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liaochong.myexcel.utils;

import com.github.liaochong.myexcel.core.annotation.ExcelColumn;
import com.github.liaochong.myexcel.core.cache.WeakCache;
import com.github.liaochong.myexcel.core.reflect.ClassFieldContainer;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author liaochong
 * @version 1.0
 */
@UtilityClass
public final class ReflectUtil {

    private static final WeakCache<Class<?>, Map<Integer, Field>> FIELD_CACHE = new WeakCache<>();

    /**
     * 获取指定类的所有字段，包含父类字段，其中
     *
     * @param clazz 类
     * @return 类的所有字段
     */
    public static ClassFieldContainer getAllFieldsOfClass(Class<?> clazz) {
        ClassFieldContainer container = new ClassFieldContainer();
        getAllFieldsOfClass(clazz, container);
        return container;
    }

    public static Map<Integer, Field> getFieldMapOfExcelColumn(Class<?> dataType) {
        Map<Integer, Field> fieldMap = FIELD_CACHE.get(dataType);
        if (Objects.nonNull(fieldMap)) {
            return fieldMap;
        }
        ClassFieldContainer classFieldContainer = ReflectUtil.getAllFieldsOfClass(dataType);
        List<Field> fields = classFieldContainer.getFieldsByAnnotation(ExcelColumn.class);
        if (fields.isEmpty()) {
            throw new IllegalStateException("There is no field with @ExcelColumn");
        }
        fieldMap = new HashMap<>(fields.size());
        for (Field field : fields) {
            ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
            int index = excelColumn.index();
            if (index < 0) {
                continue;
            }
            Field f = fieldMap.get(index);
            if (Objects.nonNull(f)) {
                throw new IllegalStateException("Index cannot be repeated. Please check it.");
            }
            field.setAccessible(true);
            fieldMap.put(index, field);
        }
        FIELD_CACHE.cache(dataType, fieldMap);
        return fieldMap;
    }

    /**
     * 根据对象以及指定字段，获取字段的值
     *
     * @param o     对象
     * @param field 指定字段
     * @return 字段值
     */
    public static Object getFieldValue(Object o, Field field) {
        if (Objects.isNull(o) || Objects.isNull(field)) {
            return null;
        }
        try {
            return field.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void getAllFieldsOfClass(Class<?> clazz, ClassFieldContainer container) {
        container.setClazz(clazz);
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            container.getDeclaredFields().add(field);
            container.getFieldMap().put(field.getName(), field);
        }
        if (clazz.getSuperclass() != null) {
            ClassFieldContainer parentContainer = new ClassFieldContainer();
            container.setParent(parentContainer);
            getAllFieldsOfClass(clazz.getSuperclass(), parentContainer);
        }
    }
}
