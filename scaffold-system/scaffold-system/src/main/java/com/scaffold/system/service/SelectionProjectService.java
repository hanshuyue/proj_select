package com.scaffold.system.service;

import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.dto.SelectionBidderRequest;
import com.scaffold.system.domain.dto.SelectionProjectRequest;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.mapper.SelectionMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SelectionProjectService {
    private final SelectionMapper mapper;
    private final ProjectAccessService access;
    private final Path reportDirectory;
    private static final java.util.Set<String> DOCUMENT_EXTENSIONS = java.util.Set.of(
            ".doc", ".docx", ".pdf", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".rtf", ".odt", ".ods", ".odp", ".csv", ".wps", ".et", ".dps"
    );

    public SelectionProjectService(SelectionMapper mapper, ProjectAccessService access,
                                   @Value("${scaffold.selection.storage-path:./data/selection}") String storagePath) {
        this.mapper = mapper;
        this.access = access;
        this.reportDirectory = Path.of(storagePath).toAbsolutePath().normalize().resolve("review-reports");
    }

    public PageResult<Map<String, Object>> list(PageQuery query, String keyword) {
        int pageNum = query.normalizedPageNum();
        int pageSize = query.normalizedPageSize();
        String owner = access.ownerFilter();
        return PageResult.of(mapper.selectProjects(keyword, (pageNum - 1) * pageSize, pageSize, owner),
                mapper.countProjects(keyword, owner), pageNum, pageSize);
    }

    public Map<String, Object> detail(Long id) {
        access.selection(id);
        Map<String, Object> row = mapper.selectProject(id);
        if (row == null) throw new BusinessException(404, "甄选结果不存在");
        row.put("bidders", mapper.selectBidders(id));
        return row;
    }

    @Transactional
    public IdResponse create(SelectionProjectRequest request) {
        SelectionProjectRequest normalized = normalize(request);
        Map<String, Object> holder = new LinkedHashMap<>();
        mapper.insertProject(holder, normalized, currentUsername());
        Long id = ((Number) holder.get("id")).longValue();
        saveBidders(id, normalized.bidders());
        return new IdResponse(id);
    }

    @Transactional
    public void update(Long id, SelectionProjectRequest request) {
        access.selection(id);
        SelectionProjectRequest normalized = normalize(request);
        if (mapper.updateProject(id, normalized, currentUsername()) == 0) {
            throw new BusinessException(404, "甄选结果不存在");
        }
        mapper.deleteBidders(id);
        saveBidders(id, normalized.bidders());
    }

    @Transactional
    public void delete(Long id) {
        access.selection(id);
        if (mapper.deleteProject(id, currentUsername()) == 0) {
            throw new BusinessException(404, "甄选结果不存在");
        }
        mapper.deleteBidders(id);
    }

    public Map<String, String> uploadReviewReport(Long id, MultipartFile file) {
        access.selection(id);
        if (mapper.selectProject(id) == null) throw new BusinessException(404, "甄选结果不存在");
        if (file == null || file.isEmpty()) throw new BusinessException("请选择评审报告");
        String original = file.getOriginalFilename() == null ? "评审报告" : file.getOriginalFilename();
        String lower = original.toLowerCase();
        int dot = lower.lastIndexOf('.');
        String suffix = dot < 0 ? "" : lower.substring(dot);
        if (!DOCUMENT_EXTENSIONS.contains(suffix)) {
            throw new BusinessException("请上传文档文件，不支持该文件格式");
        }
        try {
            Files.createDirectories(reportDirectory);
            Path target = reportDirectory.resolve(id + "_" + System.currentTimeMillis() + suffix).normalize();
            if (!target.startsWith(reportDirectory)) throw new BusinessException("附件路径无效");
            file.transferTo(target);
            Map<String, Object> old = mapper.selectReviewReport(id);
            mapper.updateReviewReport(id, original, target.toString(), currentUsername());
            deleteOldReport(old, target);
            return Map.of("filename", original);
        } catch (IOException e) {
            throw new BusinessException("评审报告保存失败：" + e.getMessage());
        }
    }

    public Map.Entry<String, byte[]> downloadReviewReport(Long id) {
        access.selection(id);
        Map<String, Object> row = mapper.selectReviewReport(id);
        if (row == null || row.get("reviewReportPath") == null) throw new BusinessException(404, "未上传评审报告");
        Path path = Path.of(String.valueOf(row.get("reviewReportPath"))).toAbsolutePath().normalize();
        if (!path.startsWith(reportDirectory) || !Files.isRegularFile(path)) {
            throw new BusinessException(404, "评审报告文件不存在");
        }
        try {
            return Map.entry(String.valueOf(row.get("reviewReportName")), Files.readAllBytes(path));
        } catch (IOException e) {
            throw new BusinessException("评审报告读取失败：" + e.getMessage());
        }
    }

    private void deleteOldReport(Map<String, Object> old, Path current) {
        if (old == null || old.get("reviewReportPath") == null) return;
        Path oldPath = Path.of(String.valueOf(old.get("reviewReportPath"))).toAbsolutePath().normalize();
        if (oldPath.startsWith(reportDirectory) && !oldPath.equals(current)) {
            try { Files.deleteIfExists(oldPath); } catch (IOException ignored) { }
        }
    }

    private void saveBidders(Long projectId, List<SelectionBidderRequest> bidders) {
        for (int i = 0; i < bidders.size(); i++) mapper.insertBidder(projectId, bidders.get(i), i + 1);
    }

    private SelectionProjectRequest normalize(SelectionProjectRequest r) {
        if (r == null) throw new BusinessException("请求数据不能为空");
        String projectName = required(r.projectName(), "项目名称不能为空");
        String opportunityNo = required(r.opportunityNo(), "商机编号不能为空");
        int year = r.reportYear() == null ? java.time.Year.now().getValue() : r.reportYear();
        int month = r.reportMonth() == null ? java.time.LocalDate.now().getMonthValue() : r.reportMonth();
        if (month < 1 || month > 12) throw new BusinessException("汇报月份必须为1至12");
        List<SelectionBidderRequest> input = r.bidders() == null ? List.of() : r.bidders();
        List<SelectionBidderRequest> bidders = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            SelectionBidderRequest b = input.get(i);
            String name = required(b.bidderName(), "第" + (i + 1) + "个投标人名称不能为空");
            BigDecimal serviceRate = taxRate(b.serviceTaxRate(), "第" + (i + 1) + "个投标人的服务增值税率");
            BigDecimal resaleRate = taxRate(b.resaleTaxRate(), "第" + (i + 1) + "个投标人的代销增值税率");
            BigDecimal serviceInc = amountWithTax(b.serviceExTax(), serviceRate, b.serviceIncTax());
            BigDecimal resaleInc = amountWithTax(b.resaleExTax(), resaleRate, b.resaleIncTax());
            BigDecimal total = b.totalScore() != null ? b.totalScore() : nz(b.priceScore()).add(nz(b.businessScore()));
            bidders.add(new SelectionBidderRequest(b.id(), name, b.serviceExTax(), serviceRate, serviceInc,
                    b.resaleExTax(), resaleRate, resaleInc, b.feeAmount(), b.priceScore(),
                    b.businessScore(), total, b.ranking() == null ? i + 1 : b.ranking()));
        }
        bidders.sort(java.util.Comparator.comparing(SelectionBidderRequest::totalScore).reversed());
        List<SelectionBidderRequest> ranked = new ArrayList<>();
        for (int i = 0; i < bidders.size(); i++) {
            SelectionBidderRequest b = bidders.get(i);
            ranked.add(new SelectionBidderRequest(b.id(), b.bidderName(), b.serviceExTax(), b.serviceTaxRate(),
                    b.serviceIncTax(), b.resaleExTax(), b.resaleTaxRate(), b.resaleIncTax(), b.feeAmount(),
                    b.priceScore(), b.businessScore(), b.totalScore(), i + 1));
        }
        return new SelectionProjectRequest(projectName, opportunityNo, trim(r.departmentName()), year, month,
                r.serviceLimitExTax(), r.serviceLimitIncTax(), taxRate(r.serviceTaxRate(), "服务增值税率"),
                r.resaleBudgetExTax(), r.resaleBudgetIncTax(), taxRate(r.resaleTaxRate(), "代销增值税率"),
                r.feeMinExTax(), r.feeMinIncTax(), taxRate(r.feeTaxRate(), "手续费增值税率"),
                trim(r.selectedCompany()), trim(r.candidateCompany()),
                defaultText(r.publicityMethod(), "网站"),
                defaultText(r.publicityWebsite(), "https://xe.sd.chinamobile.com/pms-portal-react/#/console4"),
                r.publicityEnabled() == null || r.publicityEnabled(), trim(r.executionMethod()),
                trim(r.cooperationCompany()), trim(r.expansionProject()),
                r.status() == null ? "DRAFT" : r.status(), ranked);
    }

    private BigDecimal amountWithTax(BigDecimal exTax, BigDecimal rate, BigDecimal supplied) {
        if (exTax == null) return supplied;
        return exTax.multiply(BigDecimal.ONE.add(nz(rate).movePointLeft(2))).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private BigDecimal taxRate(BigDecimal value, String label) {
        if (value == null) return BigDecimal.valueOf(6);
        if (value.compareTo(BigDecimal.valueOf(6)) != 0 && value.compareTo(BigDecimal.valueOf(9)) != 0) {
            throw new BusinessException(label + "只能选择6%或9%");
        }
        return value;
    }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String defaultText(String value, String fallback) {
        String text = trim(value);
        return text == null || text.isEmpty() ? fallback : text;
    }
    private String required(String value, String message) {
        String v = trim(value);
        if (v == null || v.isEmpty()) throw new BusinessException(message);
        return v;
    }
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : String.valueOf(auth.getPrincipal());
    }
}
