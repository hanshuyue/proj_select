package com.scaffold.system.mapper;

import com.scaffold.system.domain.dto.SelectionBidderRequest;
import com.scaffold.system.domain.dto.SelectionProjectRequest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SelectionMapper {

    @Select("""
            <script>
            SELECT COUNT(*) FROM biz_selection_project
            WHERE deleted = 0
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR project_name LIKE CONCAT('%', #{keyword}, '%')
                   OR opportunity_no LIKE CONCAT('%', #{keyword}, '%')
                   OR selected_company LIKE CONCAT('%', #{keyword}, '%'))
            </script>
            """)
    long countProjects(@Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT id, project_name AS projectName, opportunity_no AS opportunityNo,
                   department_name AS departmentName, report_year AS reportYear,
                   report_month AS reportMonth, selected_company AS selectedCompany,
                   review_report_name AS reviewReportName,
                   status, DATE_FORMAT(update_time, '%Y-%m-%d %H:%i') AS updateTime
            FROM biz_selection_project
            WHERE deleted = 0
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR project_name LIKE CONCAT('%', #{keyword}, '%')
                   OR opportunity_no LIKE CONCAT('%', #{keyword}, '%')
                   OR selected_company LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY update_time DESC, id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<Map<String, Object>> selectProjects(@Param("keyword") String keyword,
                                             @Param("offset") int offset,
                                             @Param("pageSize") int pageSize);

    @Select("""
            SELECT id, project_name AS projectName, opportunity_no AS opportunityNo,
                   department_name AS departmentName, report_year AS reportYear,
                   report_month AS reportMonth, service_limit_ex_tax AS serviceLimitExTax,
                   service_limit_inc_tax AS serviceLimitIncTax, service_tax_rate AS serviceTaxRate,
                   resale_budget_ex_tax AS resaleBudgetExTax, resale_budget_inc_tax AS resaleBudgetIncTax,
                   resale_tax_rate AS resaleTaxRate, fee_min_ex_tax AS feeMinExTax,
                   fee_min_inc_tax AS feeMinIncTax, fee_tax_rate AS feeTaxRate,
                   selected_company AS selectedCompany, candidate_company AS candidateCompany,
                   publicity_method AS publicityMethod, publicity_website AS publicityWebsite,
                   publicity_enabled AS publicityEnabled,
                   execution_method AS executionMethod, cooperation_company AS cooperationCompany,
                   expansion_project AS expansionProject,
                   review_report_name AS reviewReportName, status
            FROM biz_selection_project WHERE id = #{id} AND deleted = 0
            """)
    Map<String, Object> selectProject(@Param("id") Long id);

    @Select("""
            SELECT id, bidder_name AS bidderName, service_ex_tax AS serviceExTax,
                   service_tax_rate AS serviceTaxRate, service_inc_tax AS serviceIncTax,
                   resale_ex_tax AS resaleExTax, resale_tax_rate AS resaleTaxRate,
                   resale_inc_tax AS resaleIncTax, fee_amount AS feeAmount,
                   price_score AS priceScore, business_score AS businessScore,
                   total_score AS totalScore, ranking
            FROM biz_selection_bidder WHERE project_id = #{projectId}
            ORDER BY COALESCE(ranking, 999999), sort_no, id
            """)
    List<Map<String, Object>> selectBidders(@Param("projectId") Long projectId);

    @Insert("""
            INSERT INTO biz_selection_project (
              project_name, opportunity_no, department_name, report_year, report_month,
              service_limit_ex_tax, service_limit_inc_tax, service_tax_rate,
              resale_budget_ex_tax, resale_budget_inc_tax, resale_tax_rate,
              fee_min_ex_tax, fee_min_inc_tax, fee_tax_rate,
              selected_company, candidate_company, publicity_method, publicity_website, publicity_enabled,
              execution_method, cooperation_company, expansion_project, status, create_by, update_by
            ) VALUES (
              #{r.projectName}, #{r.opportunityNo}, #{r.departmentName}, #{r.reportYear}, #{r.reportMonth},
              #{r.serviceLimitExTax}, #{r.serviceLimitIncTax}, #{r.serviceTaxRate},
              #{r.resaleBudgetExTax}, #{r.resaleBudgetIncTax}, #{r.resaleTaxRate},
              #{r.feeMinExTax}, #{r.feeMinIncTax}, #{r.feeTaxRate},
              #{r.selectedCompany}, #{r.candidateCompany}, #{r.publicityMethod}, #{r.publicityWebsite}, #{r.publicityEnabled},
              #{r.executionMethod}, #{r.cooperationCompany}, #{r.expansionProject}, #{r.status},
              #{operator}, #{operator}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insertProject(@Param("row") Map<String, Object> row,
                      @Param("r") SelectionProjectRequest request,
                      @Param("operator") String operator);

    @Update("""
            UPDATE biz_selection_project SET
              project_name=#{r.projectName}, opportunity_no=#{r.opportunityNo},
              department_name=#{r.departmentName}, report_year=#{r.reportYear}, report_month=#{r.reportMonth},
              service_limit_ex_tax=#{r.serviceLimitExTax}, service_limit_inc_tax=#{r.serviceLimitIncTax},
              service_tax_rate=#{r.serviceTaxRate}, resale_budget_ex_tax=#{r.resaleBudgetExTax},
              resale_budget_inc_tax=#{r.resaleBudgetIncTax}, resale_tax_rate=#{r.resaleTaxRate},
              fee_min_ex_tax=#{r.feeMinExTax}, fee_min_inc_tax=#{r.feeMinIncTax}, fee_tax_rate=#{r.feeTaxRate},
              selected_company=#{r.selectedCompany}, candidate_company=#{r.candidateCompany},
              publicity_method=#{r.publicityMethod}, publicity_website=#{r.publicityWebsite},
              publicity_enabled=#{r.publicityEnabled},
              execution_method=#{r.executionMethod}, cooperation_company=#{r.cooperationCompany},
              expansion_project=#{r.expansionProject}, status=#{r.status}, update_by=#{operator}
            WHERE id=#{id} AND deleted=0
            """)
    int updateProject(@Param("id") Long id, @Param("r") SelectionProjectRequest request,
                      @Param("operator") String operator);

    @Update("UPDATE biz_selection_project SET deleted=1, update_by=#{operator} WHERE id=#{id} AND deleted=0")
    int deleteProject(@Param("id") Long id, @Param("operator") String operator);

    @Delete("DELETE FROM biz_selection_bidder WHERE project_id=#{projectId}")
    void deleteBidders(@Param("projectId") Long projectId);

    @Insert("""
            INSERT INTO biz_selection_bidder (
              project_id, bidder_name, service_ex_tax, service_tax_rate, service_inc_tax,
              resale_ex_tax, resale_tax_rate, resale_inc_tax, fee_amount,
              price_score, business_score, total_score, ranking, sort_no
            ) VALUES (
              #{projectId}, #{b.bidderName}, #{b.serviceExTax}, #{b.serviceTaxRate}, #{b.serviceIncTax},
              #{b.resaleExTax}, #{b.resaleTaxRate}, #{b.resaleIncTax}, #{b.feeAmount},
              #{b.priceScore}, #{b.businessScore}, #{b.totalScore}, #{b.ranking}, #{sortNo}
            )
            """)
    void insertBidder(@Param("projectId") Long projectId, @Param("b") SelectionBidderRequest bidder,
                      @Param("sortNo") int sortNo);

    @Select("""
            SELECT id, template_name AS templateName, original_filename AS originalFilename,
                   file_size AS fileSize, placeholder_keys AS placeholderKeys,
                   default_flag AS defaultFlag, status,
                   DATE_FORMAT(create_time, '%Y-%m-%d %H:%i') AS createTime
            FROM biz_selection_ppt_template ORDER BY default_flag DESC, create_time DESC
            """)
    List<Map<String, Object>> selectTemplates();

    @Select("""
            SELECT id, template_name AS templateName, original_filename AS originalFilename,
                   storage_path AS storagePath, file_size AS fileSize, sha256,
                   placeholder_keys AS placeholderKeys, default_flag AS defaultFlag, status
            FROM biz_selection_ppt_template WHERE id=#{id}
            """)
    Map<String, Object> selectTemplate(@Param("id") Long id);

    @Select("""
            SELECT id, template_name AS templateName, original_filename AS originalFilename,
                   storage_path AS storagePath, file_size AS fileSize, sha256,
                   placeholder_keys AS placeholderKeys, default_flag AS defaultFlag, status
            FROM biz_selection_ppt_template WHERE default_flag=1 AND status=1
            ORDER BY id DESC LIMIT 1
            """)
    Map<String, Object> selectDefaultTemplate();

    @Insert("""
            INSERT INTO biz_selection_ppt_template
              (template_name, original_filename, storage_path, file_size, sha256,
               placeholder_keys, default_flag, status, create_by)
            VALUES (#{row.templateName}, #{row.originalFilename}, #{row.storagePath}, #{row.fileSize},
                    #{row.sha256}, #{row.placeholderKeys}, #{row.defaultFlag}, 1, #{operator})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "row.id")
    int insertTemplate(@Param("row") Map<String, Object> row, @Param("operator") String operator);

    @Update("UPDATE biz_selection_ppt_template SET default_flag=0")
    void clearDefaultTemplate();

    @Update("UPDATE biz_selection_ppt_template SET default_flag=1 WHERE id=#{id} AND status=1")
    int setDefaultTemplate(@Param("id") Long id);

    @Delete("DELETE FROM biz_selection_ppt_template WHERE id=#{id}")
    int deleteTemplate(@Param("id") Long id);

    @Update("""
            UPDATE biz_selection_project
            SET review_report_name=#{filename}, review_report_path=#{path}, update_by=#{operator}
            WHERE id=#{id} AND deleted=0
            """)
    int updateReviewReport(@Param("id") Long id, @Param("filename") String filename,
                           @Param("path") String path, @Param("operator") String operator);

    @Select("""
            SELECT review_report_name AS reviewReportName, review_report_path AS reviewReportPath
            FROM biz_selection_project WHERE id=#{id} AND deleted=0
            """)
    Map<String, Object> selectReviewReport(@Param("id") Long id);
}
