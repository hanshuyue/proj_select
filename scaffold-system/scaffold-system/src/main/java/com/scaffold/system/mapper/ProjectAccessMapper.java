package com.scaffold.system.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ProjectAccessMapper {
    @Select("SELECT create_by FROM biz_selection_project WHERE id=#{id} AND deleted=0")
    String selectionOwner(Long id);
    @Select("SELECT create_by FROM biz_initiation_project WHERE id=#{id} AND deleted=0")
    String initiationOwner(Long id);
    @Select("SELECT project_id FROM biz_initiation_attachment WHERE id=#{id}")
    Long attachmentProject(Long id);
}
