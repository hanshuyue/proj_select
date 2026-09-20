package com.scaffold.system.mapper;

import com.scaffold.system.domain.dto.InitiationProjectRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface InitiationMapper {
    @Select("""
        <script>
        SELECT COUNT(*) FROM biz_initiation_project
        WHERE deleted=0 AND (#{keyword} IS NULL OR #{keyword}=''
          OR project_name LIKE CONCAT('%',#{keyword},'%')
          OR opportunity_no LIKE CONCAT('%',#{keyword},'%')
          OR customer_name LIKE CONCAT('%',#{keyword},'%'))
        </script>
        """)
    long count(@Param("keyword") String keyword);

    @Select("""
        <script>
        SELECT id, project_name projectName, opportunity_no opportunityNo, customer_name customerName,
          customer_type customerType, industry_type industryType, department_name departmentName,
          project_manager projectManager, report_year reportYear, report_month reportMonth,
          contract_amount_inc_tax contractAmountIncTax, overall_profit_rate overallProfitRate,
          fund_risk_level fundRiskLevel, three_line_level threeLineLevel, status,
          DATE_FORMAT(update_time,'%Y-%m-%d %H:%i') updateTime
        FROM biz_initiation_project
        WHERE deleted=0 AND (#{keyword} IS NULL OR #{keyword}=''
          OR project_name LIKE CONCAT('%',#{keyword},'%')
          OR opportunity_no LIKE CONCAT('%',#{keyword},'%')
          OR customer_name LIKE CONCAT('%',#{keyword},'%'))
        ORDER BY update_time DESC,id DESC LIMIT #{offset},#{pageSize}
        </script>
        """)
    List<Map<String,Object>> list(@Param("keyword") String keyword, @Param("offset") int offset,
                                  @Param("pageSize") int pageSize);

    @Select("""
        SELECT id, project_name projectName, opportunity_no opportunityNo, customer_name customerName,
          customer_type customerType, industry_type industryType, department_name departmentName,
          project_manager projectManager, report_year reportYear, report_month reportMonth,
          agreement_years agreementYears, contract_amount_inc_tax contractAmountIncTax,
          total_revenue_inc_tax totalRevenueIncTax, total_cost_inc_tax totalCostIncTax,
          overall_profit_rate overallProfitRate, fund_risk_level fundRiskLevel,
          three_line_level threeLineLevel, status, section_data sectionData
        FROM biz_initiation_project WHERE id=#{id} AND deleted=0
        """)
    Map<String,Object> get(@Param("id") Long id);

    @Insert("""
        INSERT INTO biz_initiation_project(project_name,opportunity_no,customer_name,customer_type,
          industry_type,department_name,project_manager,report_year,report_month,agreement_years,
          contract_amount_inc_tax,total_revenue_inc_tax,total_cost_inc_tax,overall_profit_rate,
          fund_risk_level,three_line_level,status,section_data,create_by,update_by)
        VALUES(#{r.projectName},#{r.opportunityNo},#{r.customerName},#{r.customerType},
          #{r.industryType},#{r.departmentName},#{r.projectManager},#{r.reportYear},#{r.reportMonth},
          #{r.agreementYears},#{r.contractAmountIncTax},#{r.totalRevenueIncTax},#{r.totalCostIncTax},
          #{r.overallProfitRate},#{r.fundRiskLevel},#{r.threeLineLevel},#{r.status},#{sectionData},
          #{operator},#{operator})
        """)
    @Options(useGeneratedKeys=true,keyProperty="row.id")
    int insert(@Param("row") Map<String,Object> row, @Param("r") InitiationProjectRequest request,
               @Param("sectionData") String sectionData, @Param("operator") String operator);

    @Update("""
        UPDATE biz_initiation_project SET project_name=#{r.projectName},opportunity_no=#{r.opportunityNo},
          customer_name=#{r.customerName},customer_type=#{r.customerType},industry_type=#{r.industryType},
          department_name=#{r.departmentName},project_manager=#{r.projectManager},report_year=#{r.reportYear},
          report_month=#{r.reportMonth},agreement_years=#{r.agreementYears},
          contract_amount_inc_tax=#{r.contractAmountIncTax},total_revenue_inc_tax=#{r.totalRevenueIncTax},
          total_cost_inc_tax=#{r.totalCostIncTax},overall_profit_rate=#{r.overallProfitRate},
          fund_risk_level=#{r.fundRiskLevel},three_line_level=#{r.threeLineLevel},status=#{r.status},
          section_data=#{sectionData},update_by=#{operator},version=version+1
        WHERE id=#{id} AND deleted=0
        """)
    int update(@Param("id") Long id, @Param("r") InitiationProjectRequest request,
               @Param("sectionData") String sectionData, @Param("operator") String operator);

    @Update("UPDATE biz_initiation_project SET deleted=1,update_by=#{operator} WHERE id=#{id} AND deleted=0")
    int delete(@Param("id") Long id,@Param("operator") String operator);

    @Delete("DELETE FROM biz_initiation_finance_item WHERE project_id=#{projectId}")
    void deleteFinanceItems(@Param("projectId") Long projectId);

    @Select("SELECT id,attachment_type attachmentType,original_filename originalFilename,file_size fileSize,storage_path storagePath,DATE_FORMAT(create_time,'%Y-%m-%d %H:%i') createTime FROM biz_initiation_attachment WHERE project_id=#{projectId} ORDER BY id")
    List<Map<String,Object>> attachments(@Param("projectId") Long projectId);
    @Select("SELECT id,project_id projectId,attachment_type attachmentType,original_filename originalFilename,file_size fileSize,storage_path storagePath FROM biz_initiation_attachment WHERE id=#{id}")
    Map<String,Object> attachment(@Param("id") Long id);
    @Insert("INSERT INTO biz_initiation_attachment(project_id,attachment_type,original_filename,storage_path,file_size,create_by) VALUES(#{projectId},#{attachmentType},#{filename},#{path},#{size},#{operator})")
    @Options(useGeneratedKeys=true,keyProperty="row.id")
    int insertAttachment(@Param("row") Map<String,Object> row,@Param("projectId") Long projectId,@Param("attachmentType") String attachmentType,@Param("filename") String filename,@Param("path") String path,@Param("size") long size,@Param("operator") String operator);
    @Delete("DELETE FROM biz_initiation_attachment WHERE id=#{id}") int deleteAttachment(@Param("id") Long id);

    @Insert("""
        INSERT INTO biz_initiation_finance_item(project_id,item_type,mode_name,item_name,amount_inc_tax,
          tax_rate,amount_ex_tax,description,sort_no)
        VALUES(#{projectId},#{itemType},#{item.mode},#{item.name},#{item.amountIncTax},#{item.taxRate},
          #{item.amountExTax},#{item.description},#{sortNo})
        """)
    void insertFinanceItem(@Param("projectId") Long projectId,@Param("itemType") String itemType,
                           @Param("item") Map<String,Object> item,@Param("sortNo") int sortNo);

    @Select("""
        SELECT id,item_type itemType,mode_name mode,item_name name,amount_inc_tax amountIncTax,
          tax_rate taxRate,amount_ex_tax amountExTax,description
        FROM biz_initiation_finance_item WHERE project_id=#{projectId} ORDER BY item_type,sort_no,id
        """)
    List<Map<String,Object>> financeItems(@Param("projectId") Long projectId);

    @Select("""
        SELECT id,template_name templateName,original_filename originalFilename,storage_path storagePath,
          file_size fileSize,sha256,placeholder_keys placeholderKeys,default_flag defaultFlag,status,
          DATE_FORMAT(create_time,'%Y-%m-%d %H:%i') createTime
        FROM biz_initiation_ppt_template ORDER BY default_flag DESC,create_time DESC
        """)
    List<Map<String,Object>> templates();

    @Select("SELECT * FROM biz_initiation_ppt_template WHERE id=#{id}")
    Map<String,Object> template(@Param("id") Long id);

    @Select("SELECT * FROM biz_initiation_ppt_template WHERE default_flag=1 AND status=1 ORDER BY id DESC LIMIT 1")
    Map<String,Object> defaultTemplate();

    @Insert("""
        INSERT INTO biz_initiation_ppt_template(template_name,original_filename,storage_path,file_size,sha256,
          placeholder_keys,default_flag,status,create_by)
        VALUES(#{row.templateName},#{row.originalFilename},#{row.storagePath},#{row.fileSize},#{row.sha256},
          #{row.placeholderKeys},#{row.defaultFlag},1,#{operator})
        """)
    @Options(useGeneratedKeys=true,keyProperty="row.id")
    int insertTemplate(@Param("row") Map<String,Object> row,@Param("operator") String operator);

    @Update("UPDATE biz_initiation_ppt_template SET default_flag=0")
    void clearDefault();
    @Update("UPDATE biz_initiation_ppt_template SET default_flag=1 WHERE id=#{id} AND status=1")
    int setDefault(@Param("id") Long id);
    @Delete("DELETE FROM biz_initiation_ppt_template WHERE id=#{id} AND default_flag=0")
    int deleteTemplate(@Param("id") Long id);
}
