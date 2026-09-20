package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.vo.IdResponse;
import com.scaffold.system.mapper.SelectionMapper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SelectionTemplateService {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{[#/]?([a-zA-Z][\\w.]*)}}");
    private final SelectionMapper mapper;
    private final Path storageRoot;
    private final long maxTemplateSize;

    public SelectionTemplateService(SelectionMapper mapper,
                                    @Value("${scaffold.selection.storage-path:./data/selection}") String storagePath,
                                    @Value("${scaffold.selection.max-template-size:20971520}") long maxTemplateSize) {
        this.mapper = mapper;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.maxTemplateSize = maxTemplateSize;
    }

    public List<Map<String, Object>> list() { return mapper.selectTemplates(); }

    public IdResponse upload(String name, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择PPTX模板文件");
        String original = file.getOriginalFilename() == null ? "template.pptx" : file.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".pptx")) throw new BusinessException("仅支持.pptx模板");
        if (file.getSize() > maxTemplateSize) throw new BusinessException("模板文件不能超过20MB");
        try {
            Files.createDirectories(storageRoot.resolve("templates"));
            return save(name, original, file.getBytes());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("PPTX模板解析失败：" + e.getMessage());
        }
    }

    public IdResponse importLocalTemplate(String name, Path source) {
        try {
            return save(name, source.getFileName().toString(), Files.readAllBytes(source));
        } catch (Exception e) {
            throw new BusinessException("初始化PPT模板失败：" + e.getMessage());
        }
    }

    private IdResponse save(String name, String original, byte[] bytes) throws Exception {
        Set<String> keys = scan(bytes);
        String sha = hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        String storedName = System.currentTimeMillis() + "-" + sha.substring(0, 12) + ".pptx";
        Path target = storageRoot.resolve("templates").resolve(storedName).normalize();
        if (!target.startsWith(storageRoot)) throw new BusinessException("非法文件路径");
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("templateName", name == null || name.isBlank() ? original : name.trim());
        row.put("originalFilename", original);
        row.put("storagePath", target.toString());
        row.put("fileSize", bytes.length);
        row.put("sha256", sha);
        row.put("placeholderKeys", String.join(",", keys));
        row.put("defaultFlag", mapper.selectDefaultTemplate() == null ? 1 : 0);
        mapper.insertTemplate(row, currentUsername());
        return new IdResponse(((Number) row.get("id")).longValue());
    }

    public void setDefault(Long id) {
        if (mapper.selectTemplate(id) == null) throw new BusinessException(404, "模板不存在");
        mapper.clearDefaultTemplate();
        if (mapper.setDefaultTemplate(id) == 0) throw new BusinessException("模板不可用");
    }

    public Map<String, Object> get(Long id) {
        Map<String, Object> row = mapper.selectTemplate(id);
        if (row == null) throw new BusinessException(404, "模板不存在");
        return row;
    }

    public byte[] download(Long id) {
        try {
            return Files.readAllBytes(Path.of(String.valueOf(get(id).get("storagePath"))));
        } catch (Exception e) {
            throw new BusinessException("模板文件不存在或无法读取");
        }
    }

    public void delete(Long id) {
        Map<String, Object> row = get(id);
        if (asBoolean(row.get("defaultFlag"))) throw new BusinessException("默认模板不能删除，请先设置其他默认模板");
        try { Files.deleteIfExists(Path.of(String.valueOf(row.get("storagePath")))); }
        catch (Exception ignored) { }
        mapper.deleteTemplate(id);
    }

    private Set<String> scan(byte[] bytes) throws Exception {
        Set<String> result = new LinkedHashSet<>();
        try (XMLSlideShow ppt = new XMLSlideShow(new java.io.ByteArrayInputStream(bytes))) {
            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape text) collect(text.getText(), result);
                    if (shape instanceof XSLFTable table) {
                        for (XSLFTableRow row : table.getRows())
                            for (XSLFTableCell cell : row.getCells()) collect(cell.getText(), result);
                    }
                }
            }
        }
        return result;
    }

    private void collect(String text, Set<String> result) {
        Matcher matcher = TOKEN.matcher(text == null ? "" : text);
        while (matcher.find()) result.add(matcher.group(1));
    }
    private boolean asBoolean(Object value) {
        return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() == 1;
    }
    private String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) s.append(String.format("%02x", b));
        return s.toString();
    }
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : String.valueOf(auth.getPrincipal());
    }
}
