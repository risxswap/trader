package cc.riskswap.trader.collector.common.model.dto;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据结果集类，用于封装查询结果
 */
public class ResultSetDto {

    private String[] fields;
    
    private String[][] items;
    
    /**
     * 获取字段索引
     * @param fieldName 字段名
     * @return 字段索引
     */
    private int fieldIndex(String fieldName) {
        Assert.hasText(fieldName, "字段名不能为空");
        for(int i=0; i<fields.length; i++) {
            if(fieldName.equals(fields[i]))
                return i;
        }
        throw new IllegalArgumentException("找不到字段：" + fieldName);
    }
    
    /**
     * 获取字段值
     * @param fieldName 字段名
     * @param row 行索引
     * @return 字段值
     */
    public String getFieldValue(String fieldName, int row) {
        int idx = fieldIndex(fieldName);
        return items[row][idx];
    }
    
    /**
     * 设置字段值
     * @param fieldName 字段名
     * @param row 行索引
     * @param value 字段值
     */
    public void setFieldValue(String fieldName, int row, String value) {
        int idx = fieldIndex(fieldName);
        items[row][idx] = value;
    }
    
    /**
     * 重命名字段
     * @param fromName 原字段名
     * @param toName 新字段名
     */
    public void setFieldName(String fromName, String toName) {
        fields[fieldIndex(fromName)] = toName;
    }
    
    /**
     * 获取数据行数
     * @return 行数
     */
    public int size() {
        return items.length;
    }
    
    /**
     * 转换为JSON列表
     * @return JSON对象列表
     */
    public List<JSONObject> toJSONList(){
        List<JSONObject> results = new ArrayList<>(size());
        for(int i=0; i<size(); i++) {
            JSONObject json = new JSONObject();
            for(String field : getFields()) {
                json.put(field, getFieldValue(field, i));
            }
            results.add(json);
        }
        
        return results;
    }
    
    // Getters and Setters
    public String[] getFields() {
        return fields;
    }
    
    public void setFields(String[] fields) {
        this.fields = fields;
    }
    
    public String[][] getItems() {
        return items;
    }
    
    public void setItems(String[][] items) {
        this.items = items;
    }
}