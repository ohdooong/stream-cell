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

public class ListToJsonTypeHandler extends BaseTypeHandler<List<Object>> {

    private static final JsonMapper mapper = new JsonMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Object> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parseJson(parameter));
    }

    @Override
    public List<Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readValueToList(rs.getString(columnName));
    }

    @Override
    public List<Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readValueToList(rs.getString(columnIndex));
    }

    @Override
    public List<Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readValueToList(cs.getString(columnIndex));
    }

    private String parseJson(List<Object> parameter) {
        try {
            return mapper.writeValueAsString(parameter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json parsing error: ", e);
        }
    }

    private List<Object> readValueToList(String json) {
        if (json == null) return null;
        try {
            return mapper.readValue(json, new TypeReference<List<Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json read error: ", e);
        }
    }

}
