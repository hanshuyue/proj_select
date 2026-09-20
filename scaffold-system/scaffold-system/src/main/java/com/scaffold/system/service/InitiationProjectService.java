package com.scaffold.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.dto.InitiationProjectRequest;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.mapper.InitiationMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class InitiationProjectService {
    private final InitiationMapper mapper;
    private final ObjectMapper objectMapper; private final InitiationAttachmentService attachments;

    public InitiationProjectService(InitiationMapper mapper,ObjectMapper objectMapper,InitiationAttachmentService attachments) {
        this.mapper=mapper; this.objectMapper=objectMapper;this.attachments=attachments;
    }

    public PageResult<Map<String,Object>> list(PageQuery q,String keyword) {
        int p=q.normalizedPageNum(),s=q.normalizedPageSize();
        return PageResult.of(mapper.list(keyword,(p-1)*s,s),mapper.count(keyword),p,s);
    }

    public Map<String,Object> detail(Long id) {
        Map<String,Object> row=mapper.get(id);
        if(row==null) throw new BusinessException(404,"立项项目不存在");
        try {
            Object raw=row.remove("sectionData");
            row.put("sections",raw==null?new LinkedHashMap<>():
                    objectMapper.readValue(String.valueOf(raw),new TypeReference<Map<String,Object>>(){}));
        } catch(Exception e) { throw new BusinessException("立项章节数据解析失败"); }
        List<Map<String,Object>> income=new ArrayList<>(),cost=new ArrayList<>();
        for(Map<String,Object> item:mapper.financeItems(id)) {
            if("INCOME".equals(item.remove("itemType"))) income.add(item); else cost.add(item);
        }
        row.put("incomeItems",income); row.put("costItems",cost);
        row.put("attachments",attachments.list(id));
        return row;
    }

    @Transactional
    public IdResponse create(InitiationProjectRequest input) {
        InitiationProjectRequest r=normalize(input);
        Map<String,Object> holder=new HashMap<>();
        mapper.insert(holder,r,json(r.sections()),operator());
        Long id=((Number)holder.get("id")).longValue();
        saveItems(id,r);
        return new IdResponse(id);
    }

    @Transactional
    public void update(Long id,InitiationProjectRequest input) {
        InitiationProjectRequest r=normalize(input);
        if(mapper.update(id,r,json(r.sections()),operator())==0) throw new BusinessException(404,"立项项目不存在");
        mapper.deleteFinanceItems(id); saveItems(id,r);
    }

    @Transactional
    public void delete(Long id) {
        if(mapper.delete(id,operator())==0) throw new BusinessException(404,"立项项目不存在");
        mapper.deleteFinanceItems(id);
    }

    public Map<String,Object> calculate(InitiationProjectRequest input) {
        BigDecimal revenue=sum(input==null?null:input.incomeItems());
        BigDecimal cost=sum(input==null?null:input.costItems());
        BigDecimal profit=revenue.subtract(cost);
        BigDecimal rate=revenue.signum()==0?BigDecimal.ZERO:
                profit.multiply(BigDecimal.valueOf(100)).divide(revenue,2,RoundingMode.HALF_UP);
        return Map.of("totalRevenueIncTax",revenue,"totalCostIncTax",cost,
                "profit",profit,"overallProfitRate",rate);
    }

    public List<String> validate(Long id) {
        Map<String,Object> p=detail(id); List<String> errors=new ArrayList<>();
        required(p,"projectName","项目名称",errors); required(p,"opportunityNo","商机编号",errors);
        required(p,"customerName","客户名称",errors);
        if(((List<?>)p.get("incomeItems")).isEmpty()) errors.add("至少填写一条收益明细");
        if(((List<?>)p.get("costItems")).isEmpty()) errors.add("至少填写一条支出明细");
        if (!hasRequiredFusionC(p)) errors.add("\u878dC\u4e3a\u5fc5\u9009\uff0c\u96c6\u56e2\u6210\u5458\u53f7\u5361\uff08\u4fdd\uff09\u548c\u96c6\u56e2\u6210\u5458\u53f7\u5361\uff08\u62d3\uff09\u81f3\u5c11\u586b\u5199\u4e00\u9879");
        return errors;
    }

    private boolean hasRequiredFusionC(Map<String,Object> project) {
        Object sectionsRaw = project.get("sections");
        if (!(sectionsRaw instanceof Map<?,?> sections)) return false;
        Object tableRaw = sections.get("fusionRightTable");
        if (!(tableRaw instanceof Map<?,?> table)) return false;
        for (String key : List.of("\u96c6\u56e2\u6210\u5458\u53f7\u5361\uff08\u4fdd\uff09", "\u96c6\u56e2\u6210\u5458\u53f7\u5361\uff08\u62d3\uff09")) {
            Object rowRaw = table.get(key);
            if (rowRaw instanceof Map<?,?> row && row.values().stream().anyMatch(v -> v != null && !String.valueOf(v).isBlank())) return true;
        }
        return false;
    }

    private void required(Map<String,Object> p,String key,String label,List<String> errors) {
        if(p.get(key)==null||String.valueOf(p.get(key)).isBlank()) errors.add(label+"不能为空");
    }
    private void saveItems(Long id,InitiationProjectRequest r) {
        save(id,"INCOME",r.incomeItems()); save(id,"COST",r.costItems());
    }
    private void save(Long id,String type,List<Map<String,Object>> items) {
        if(items==null)return; for(int i=0;i<items.size();i++) mapper.insertFinanceItem(id,type,items.get(i),i+1);
    }
    private BigDecimal sum(List<Map<String,Object>> items) {
        if(items==null)return BigDecimal.ZERO; BigDecimal total=BigDecimal.ZERO;
        for(Map<String,Object> i:items) {
            if ("小计".equals(i.get("name")) || "合计".equals(i.get("mode"))) continue;
            total=total.add(decimal(i.get("amountIncTax")));
        }
        return total.setScale(2,RoundingMode.HALF_UP);
    }
    private BigDecimal decimal(Object v) {
        if(v==null||String.valueOf(v).isBlank())return BigDecimal.ZERO;
        try{return new BigDecimal(String.valueOf(v));}catch(Exception e){return BigDecimal.ZERO;}
    }
    private InitiationProjectRequest normalize(InitiationProjectRequest r) {
        if(r==null) throw new BusinessException("请求数据不能为空");
        String name=req(r.projectName(),"项目名称不能为空"),no=req(r.opportunityNo(),"商机编号不能为空");
        Map<String,Object> calculated=calculate(r);
        BigDecimal revenue=(BigDecimal)calculated.get("totalRevenueIncTax");
        BigDecimal cost=(BigDecimal)calculated.get("totalCostIncTax");
        BigDecimal profit=revenue.subtract(cost);
        BigDecimal profitRate=revenue.signum()==0?BigDecimal.ZERO:
                profit.multiply(BigDecimal.valueOf(100)).divide(revenue,2,RoundingMode.HALF_UP);
        String customer = r.sections() != null && r.sections().get("projectSubject") != null
                ? String.valueOf(r.sections().get("projectSubject")) : r.customerName();
        return new InitiationProjectRequest(name,no,trim(customer),trim(r.customerType()),
          trim(r.industryType()),trim(r.departmentName()),trim(r.projectManager()),
          r.reportYear()==null?LocalDate.now().getYear():r.reportYear(),
          r.reportMonth()==null?LocalDate.now().getMonthValue():r.reportMonth(),
          r.agreementYears(),r.contractAmountIncTax(),
                revenue, cost, profitRate,trim(r.fundRiskLevel()),trim(r.threeLineLevel()),
          r.status()==null?"DRAFT":r.status(),r.sections()==null?new LinkedHashMap<>():r.sections(),
          r.incomeItems()==null?List.of():r.incomeItems(),r.costItems()==null?List.of():r.costItems());
    }
    private String req(String s,String msg){s=trim(s);if(s==null||s.isEmpty())throw new BusinessException(msg);return s;}
    private String trim(String s){return s==null?null:s.trim();}
    private String json(Object o){try{return objectMapper.writeValueAsString(o);}catch(Exception e){throw new BusinessException("章节数据序列化失败");}}
    private String operator(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":String.valueOf(a.getPrincipal());}
}
