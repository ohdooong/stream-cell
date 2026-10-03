package com.streamcell.global.helper.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.streamcell.platform.pipeline.vo.ProgramArgs;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(ProgramArgs.class)
@MappedJdbcTypes({
        JdbcType.VARCHAR,
        JdbcType.LONGVARCHAR,
        JdbcType.OTHER
})
public class ProgramArgsTypeHandler extends BaseTypeHandler<ProgramArgs> {

    private static final JsonMapper mapper = JsonMapper.builder()
            .build();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ProgramArgs parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, writeJson(parameter));
    }

    @Override
    public ProgramArgs getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readJson(rs.getString(columnName));
    }

    @Override
    public ProgramArgs getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readJson(rs.getString(columnIndex));
    }

    @Override
    public ProgramArgs getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readJson(cs.getString(columnIndex));
    }

    private String writeJson(ProgramArgs programArgs) throws SQLException {
        try {
            return mapper.writeValueAsString(programArgs);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize ProgramArgs.", e);
        }
    }

    private ProgramArgs readJson(String json) throws SQLException {

        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return mapper.readValue(json, ProgramArgs.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize ProgramArgs.", e);
        }
    }
}