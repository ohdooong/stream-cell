package com.streamcell.platform.ai.repository;

import com.streamcell.platform.ai.dto.PipelineResultTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PipelineResultRepository {

    @Update("""
            CREATE TABLE IF NOT EXISTS platform.#{tableName} (
                #{columns}
            );
            """)
    void createPipelineResultTable(PipelineResultTable dto);
}
