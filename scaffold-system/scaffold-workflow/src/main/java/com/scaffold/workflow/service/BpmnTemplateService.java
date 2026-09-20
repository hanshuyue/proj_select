package com.scaffold.workflow.service;

import com.scaffold.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BpmnTemplateService {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]{1,99}");

    public String defaultXml(String processKey, String processName) {
        String key = safeKey(processKey);
        String name = xml(processName == null || processName.isBlank() ? processKey : processName);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://scaffold.workflow">
                  <process id="%s" name="%s" isExecutable="true">
                    <startEvent id="startEvent" name="提交申请"/>
                    <sequenceFlow id="flow_start_dept" sourceRef="startEvent" targetRef="deptApprove"/>
                    <userTask id="deptApprove" name="部门负责人审批" flowable:candidateGroups="super_admin,sys_admin"/>
                    <sequenceFlow id="flow_dept_finance" sourceRef="deptApprove" targetRef="financeApprove"/>
                    <userTask id="financeApprove" name="财务复核" flowable:candidateGroups="super_admin,biz_admin"/>
                    <sequenceFlow id="flow_finance_end" sourceRef="financeApprove" targetRef="endEvent"/>
                    <endEvent id="endEvent" name="归档"/>
                  </process>
                </definitions>
                """.formatted(key, name);
    }

    public String normalizeXml(String bpmnXml, String processKey, String processName) {
        if (bpmnXml == null || bpmnXml.isBlank()) {
            return defaultXml(processKey, processName);
        }
        String key = safeKey(processKey);
        String xml = bpmnXml.trim();
        String processId = executableProcessId(xml);
        if (!key.equals(processId)) {
            throw new BusinessException("BPMN process id 必须等于流程模型标识");
        }
        return xml;
    }

    public String safeKey(String processKey) {
        if (processKey == null || !KEY_PATTERN.matcher(processKey).matches()) {
            throw new BusinessException("流程模型标识只能包含字母、数字、下划线，并且必须以字母开头");
        }
        return processKey;
    }

    public List<String> nodeNames(String bpmnXml) {
        List<String> names = new ArrayList<>();
        try {
            Document document = parse(bpmnXml);
            collectNames(document, names, "startEvent");
            collectNames(document, names, "userTask");
            collectNames(document, names, "endEvent");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception ignored) {
            return List.of("提交申请", "审批处理", "归档");
        }
        return names.isEmpty() ? List.of("提交申请", "审批处理", "归档") : names;
    }

    private String xml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String executableProcessId(String bpmnXml) {
        try {
            Document document = parse(bpmnXml);
            NodeList processes = document.getElementsByTagNameNS("*", "process");
            for (int i = 0; i < processes.getLength(); i++) {
                Element process = (Element) processes.item(i);
                if ("true".equals(process.getAttribute("isExecutable"))) {
                    String id = process.getAttribute("id");
                    if (id == null || id.isBlank()) {
                        throw new BusinessException("BPMN可执行流程必须包含process id");
                    }
                    return id;
                }
            }
            throw new BusinessException("BPMN XML必须包含可执行流程定义");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("BPMN XML格式不正确");
        }
    }

    private Document parse(String bpmnXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(bpmnXml)));
    }

    private void collectNames(Document document, List<String> names, String tagName) {
        NodeList nodes = document.getElementsByTagNameNS("*", tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            String name = element.getAttribute("name");
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
    }
}
