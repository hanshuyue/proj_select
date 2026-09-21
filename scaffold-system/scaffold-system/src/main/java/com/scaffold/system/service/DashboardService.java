package com.scaffold.system.service;

import com.scaffold.system.mapper.DashboardMapper;
import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final DashboardMapper mapper;
    private final PermissionResolver permissions;

    public DashboardService(DashboardMapper mapper, PermissionResolver permissions) {
        this.mapper = mapper;
        this.permissions = permissions;
    }

    public Map<String, Object> summary(String range) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(401, "请先登录");
        }
        String username = authentication.getName();
        int hours = switch (range) {
            case "7d" -> 168;
            case "30d" -> 720;
            default -> 24;
        };
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stats", stats());
        data.put("trend", trend(hours));
        data.put("todos", mapper.selectTodos(permissions.hasPermission(username, "workflow:todo:list") ? null : username));
        data.put("activities", mapper.selectActivities(permissions.hasPermission(username, "monitor:loginlog:list") ? null : username));
        data.put("roleDist", mapper.selectRoleDistribution());

        long totalCalls = mapper.countApiCallsInHours(hours);
        long successCalls = mapper.countApiCallsSuccessInHours(hours);
        data.put("trendTotal", totalCalls);
        data.put("successRate", totalCalls > 0 ? Math.round(successCalls * 1000.0 / totalCalls) / 10.0 : 100.0);

        return data;
    }

    private List<Map<String, Object>> stats() {
        long users = mapper.countUsers();
        long online = mapper.countOnlineUsers();
        long todo = mapper.countTodoTasks();
        long apiCalls = mapper.countApiCalls();

        long usersNew = mapper.countUsersCreatedSince1d();
        long online2h = mapper.countOnlineUsers2h();
        long todoNew = mapper.countTodoTasksSince1d();
        long apiPrev = mapper.countApiCallsPrev24h();

        return List.of(
                stat("users", "用户总数", users, "users", "ok", delta(users, usersNew)),
                stat("online", "在线用户", online, "monitor", "info", delta(online, online2h)),
                stat("todo", "待办任务", todo, "bell", "warn", delta(todo, todoNew)),
                stat("api", "24h 接口调用", apiCalls, "command", "purple", deltaCompare(apiCalls, apiPrev))
        );
    }

    private Map<String, Object> stat(String key, String label, long value, String icon, String tone, double[] deltaInfo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("label", label);
        row.put("value", value);
        row.put("icon", icon);
        row.put("tone", tone);
        row.put("delta", deltaInfo[0]);
        row.put("up", deltaInfo[1] > 0);
        row.put("sub", subText(key));
        return row;
    }

    private double[] delta(long total, long recent) {
        if (total == 0) return new double[]{0, 0};
        double pct = Math.round(recent * 1000.0 / Math.max(total, 1)) / 10.0;
        return new double[]{pct, 1};
    }

    private double[] deltaCompare(long current, long previous) {
        if (previous == 0) return new double[]{current > 0 ? 100.0 : 0, current > 0 ? 1 : 0};
        double pct = Math.round((current - previous) * 1000.0 / previous) / 10.0;
        return new double[]{Math.abs(pct), pct >= 0 ? 1 : -1};
    }

    private String subText(String key) {
        return switch (key) {
            case "users" -> "当前启用账号";
            case "online" -> "最近 1 小时登录";
            case "todo" -> "待处理审批任务";
            case "api" -> "最近 24 小时操作";
            default -> "";
        };
    }

    private List<Integer> trend(int hours) {
        int buckets = Math.min(hours, 24);
        Map<Integer, Integer> totalsByBucket = new LinkedHashMap<>();

        if (hours <= 24) {
            for (Map<String, Object> row : mapper.selectLoginTrend()) {
                Number hourVal = (Number) row.get("hourValue");
                Number totalVal = (Number) row.get("total");
                if (hourVal != null && totalVal != null) {
                    totalsByBucket.put(hourVal.intValue(), totalVal.intValue());
                }
            }
            List<Integer> values = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 23; i >= 0; i--) {
                int hour = now.minusHours(i).getHour();
                values.add(totalsByBucket.getOrDefault(hour, 0));
            }
            return values;
        }

        int days = hours / 24;
        for (Map<String, Object> row : mapper.selectLoginTrendByDay(days)) {
            Number dayIdx = (Number) row.get("dayIndex");
            Number totalVal = (Number) row.get("total");
            if (dayIdx != null && totalVal != null) {
                totalsByBucket.put(dayIdx.intValue(), totalVal.intValue());
            }
        }
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            values.add(totalsByBucket.getOrDefault(i, 0));
        }
        return values;
    }
}
