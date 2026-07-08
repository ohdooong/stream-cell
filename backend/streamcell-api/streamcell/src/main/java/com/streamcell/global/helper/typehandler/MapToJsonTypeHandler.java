package com.streamcell.global.helper.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MapToJsonTypeHandler extends BaseTypeHandler<Map<String, String>> {

    private static final JsonMapper mapper = new JsonMapper();


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parseJson(parameter));
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readValueToMap(rs.getString(columnName));
    }

    @Override
    public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readValueToMap(rs.getString(columnIndex));
    }

    @Override
    public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readValueToMap(cs.getString(columnIndex));
    }

    private String parseJson(Map<String, String> parameter) {
        try {
            return mapper.writeValueAsString(parameter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json parsing error: ", e);
        }
    }

    private Map<String, String> readValueToMap(String json) {
        if (json == null) return null;
        try {
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json read error: ", e);
        }
    }
}
