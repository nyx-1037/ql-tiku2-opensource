package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.entity.AiChatRecord;
import com.qltiku2.entity.AiChatSession;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.AiChatSessionMapper;
import com.qltiku2.mapper.SysUserMapper;
import com.qltiku2.service.AiChatService;
import com.qltiku2.service.AiQuotaService;
import com.qltiku2.utils.JwtUtils;
import com.qltiku2.vo.AiChatSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI聊天控制器
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private AiQuotaService aiQuotaService;

    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private AiChatSessionMapper aiChatSessionMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 发送消息给AI（流式响应）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // 临时注释用于调试
    public Flux<String> chatStream(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            log.info("=== AI聊天流式响应开始 ===");
            
            // 获取当前安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();
            
            // 获取用户ID
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                log.error("无法获取用户ID，请求未授权");
                return Flux.just("data: {\"error\":\"未授权访问，请重新登录\"}\n\n", "data: [DONE]\n\n");
            }
            
            String sessionId = (String) request.get("sessionId");
            String message = (String) request.get("message");
            Long questionId = request.get("questionId") != null ? 
                Long.valueOf(request.get("questionId").toString()) : null;
            Long modelId = request.get("modelId") != null ? 
                Long.valueOf(request.get("modelId").toString()) : null;

            log.info("请求参数: userId={}, sessionId={}, message={}, modelId={}", userId, sessionId, message, modelId);

            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
                log.info("生成新的sessionId: {}", sessionId);
            }

            // 检查并消耗配额
            boolean hasQuota = aiQuotaService.hasQuota(userId, "chat");
            if (!hasQuota) {
                log.warn("用户{}AI对话配额不足", userId);
                return Flux.just("data: {\"content\":\"今日AI对话次数已用完，请明天再试或联系管理员\"}\n\n", "data: [DONE]\n\n");
            }
            
            boolean consumed = aiQuotaService.consumeQuota(userId, "chat", 1);
            if (!consumed) {
                log.warn("用户{}AI配额消耗失败", userId);
                return Flux.just("data: {\"content\":\"配额消耗失败，请稍后重试\"}\n\n", "data: [DONE]\n\n");
            }

            log.info("开始调用AI服务...");
            return aiChatService.sendMessage(userId, sessionId, message, questionId, modelId)
                    .doOnNext(content -> log.debug("AI响应内容: {}", content))
                    .map(content -> {
                        try {
                            // 将内容包装为JSON格式
                            String jsonContent = "{\"content\":\"" + 
                                content.replace("\"", "\\\"")
                                      .replace("\n", "\\n")
                                      .replace("\r", "\\r") + 
                                "\"}";
                            return "data: " + jsonContent + "\n\n";
                        } catch (Exception e) {
                            log.error("格式化响应内容失败", e);
                            return "data: {\"content\":\"" + content + "\"}\n\n";
                        }
                    })
                    .concatWith(Flux.just("data: [DONE]\n\n"))
                    .doOnComplete(() -> log.info("AI聊天流式响应完成"))
                    .doOnError(error -> log.error("AI聊天流式响应错误", error))
                    .contextWrite(Context.of(SecurityContext.class, securityContext));

        } catch (Exception e) {
            log.error("AI聊天流式响应失败", e);
            return Flux.just("data: {\"error\":\"抱歉，AI服务暂时不可用: " + e.getMessage() + "\"}\n\n", "data: [DONE]\n\n");
        }
    }

    /**
     * 题目解析（流式响应）
     */
    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // 临时注释用于调试
    public Flux<String> analyzeQuestionStream(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            log.info("=== AI题目解析流式响应开始 ===");
            
            // 获取当前安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();
            
            // 获取用户ID
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                log.error("无法获取用户ID，请求未授权");
                return Flux.just("data: {\"error\":\"未授权访问，请重新登录\"}\n\n", "data: [DONE]\n\n");
            }
            
            String sessionId = (String) request.get("sessionId");
            String questionContent = (String) request.get("questionContent");
            String question = (String) request.get("question"); // 兼容前端传参
            String options = (String) request.get("options");
            Long questionId = request.get("questionId") != null ? 
                Long.valueOf(request.get("questionId").toString()) : null;
            Long modelId = request.get("modelId") != null ? 
                Long.valueOf(request.get("modelId").toString()) : null;

            // 兼容处理题目内容参数
            if (questionContent == null && question != null) {
                questionContent = question;
            }

            log.info("解析请求参数: userId={}, sessionId={}, questionContent={}, modelId={}", userId, sessionId, 
                questionContent != null ? questionContent.substring(0, Math.min(50, questionContent.length())) + "..." : "null", modelId);

            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
                log.info("生成新的sessionId: {}", sessionId);
            }

            // 检查并消耗配额
            boolean hasQuota = aiQuotaService.hasQuota(userId, "analyze");
            if (!hasQuota) {
                log.warn("用户{}AI解析配额不足", userId);
                return Flux.just("data: {\"content\":\"今日AI解析次数已用完，请明天再试或联系管理员\"}\n\n", "data: [DONE]\n\n");
            }
            
            boolean consumed = aiQuotaService.consumeQuota(userId, "analyze", 1);
            if (!consumed) {
                log.warn("用户{}AI解析配额消耗失败", userId);
                return Flux.just("data: {\"content\":\"配额消耗失败，请稍后重试\"}\n\n", "data: [DONE]\n\n");
            }

            log.info("开始调用AI解析服务...");
            return aiChatService.analyzeQuestion(userId, sessionId, questionContent, options, questionId, modelId)
                    .doOnNext(content -> log.debug("AI解析响应内容: {}", content))
                    .map(content -> {
                        try {
                            // 将内容包装为JSON格式
                            String jsonContent = "{\"content\":\"" + 
                                content.replace("\"", "\\\"")
                                      .replace("\n", "\\n")
                                      .replace("\r", "\\r") + 
                                "\"}";
                            return "data: " + jsonContent + "\n\n";
                        } catch (Exception e) {
                            log.error("格式化响应内容失败", e);
                            return "data: {\"content\":\"" + content + "\"}\n\n";
                        }
                    })
                    .concatWith(Flux.just("data: [DONE]\n\n"))
                    .doOnComplete(() -> log.info("AI题目解析流式响应完成"))
                    .doOnError(error -> log.error("AI题目解析流式响应错误", error))
                    .contextWrite(Context.of(SecurityContext.class, securityContext));

        } catch (Exception e) {
            log.error("AI题目解析流式响应失败", e);
            return Flux.just("data: {\"error\":\"抱歉，AI解析服务暂时不可用: " + e.getMessage() + "\"}\n\n", "data: [DONE]\n\n");
        }
    }

    /**
     * AI判题（流式响应）
     */
    @PostMapping(value = "/grading/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // 临时注释用于调试
    public Flux<String> aiGradingStream(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            log.info("=== AI判题流式响应开始 ===");
            
            // 获取当前安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();
            
            // 获取用户ID
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                log.error("无法获取用户ID，请求未授权");
                return Flux.just("data: {\"error\":\"未授权访问，请重新登录\"}\n\n", "data: [DONE]\n\n");
            }
            
            Long questionId = Long.valueOf(request.get("questionId").toString());
            String questionContent = (String) request.get("questionContent");
            String userAnswer = (String) request.get("userAnswer");
            String correctAnswer = (String) request.get("correctAnswer");

            log.info("AI判题请求参数: userId={}, questionId={}, userAnswer={}", userId, questionId, userAnswer);

            // 检查并消耗配额
            boolean hasQuota = aiQuotaService.hasQuota(userId, "grading");
            if (!hasQuota) {
                log.warn("用户{}AI判题配额不足", userId);
                return Flux.just("data: {\"content\":\"今日AI判题次数已用完，请明天再试或联系管理员\"}\n\n", "data: [DONE]\n\n");
            }
            
            boolean consumed = aiQuotaService.consumeQuota(userId, "grading", 1);
            if (!consumed) {
                log.warn("用户{}AI判题配额消耗失败", userId);
                return Flux.just("data: {\"content\":\"配额消耗失败，请稍后重试\"}\n\n", "data: [DONE]\n\n");
            }

            // 构建判题提示词
            String prompt = String.format(
                "请对以下简答题进行判题评分：\n题目：%s\n用户答案：%s\n参考答案：%s\n请给出评分和详细的评价，并在最后一行明确标注：判断结果：正确 或 判断结果：错误",
                questionContent, userAnswer, correctAnswer != null ? correctAnswer : "无参考答案"
            );

            // 生成会话ID
            String sessionId = "grading_" + questionId + "_" + userId + "_" + System.currentTimeMillis();
            
            // 获取模型ID参数
            Long modelId = request.get("modelId") != null ? 
                Long.valueOf(request.get("modelId").toString()) : null;

            log.info("开始调用AI判题服务...");
            return aiChatService.sendMessage(userId, sessionId, prompt, questionId, modelId)
                    .doOnNext(content -> log.debug("AI判题响应内容: {}", content))
                    .map(content -> {
                        try {
                            // 将内容包装为JSON格式
                            String jsonContent = "{\"content\":\"" + 
                                content.replace("\"", "\\\"")
                                      .replace("\n", "\\n")
                                      .replace("\r", "\\r") + 
                                "\"}";
                            return "data: " + jsonContent + "\n\n";
                        } catch (Exception e) {
                            log.error("格式化响应内容失败", e);
                            return "data: {\"content\":\"" + content + "\"}\n\n";
                        }
                    })
                    .concatWith(Flux.just("data: [DONE]\n\n"))
                    .doOnComplete(() -> log.info("AI判题流式响应完成"))
                    .doOnError(error -> log.error("AI判题流式响应错误", error))
                    .contextWrite(Context.of(SecurityContext.class, securityContext));

        } catch (Exception e) {
            log.error("AI判题流式响应失败", e);
            return Flux.just("data: {\"error\":\"抱歉，AI判题服务暂时不可用: " + e.getMessage() + "\"}\n\n", "data: [DONE]\n\n");
        }
    }

    /**
     * 获取聊天历史记录
     */
    @GetMapping("/chat/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<List<AiChatRecord>> getChatHistory(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            List<AiChatRecord> history = aiChatService.getChatHistory(userId, sessionId, limit);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return Result.error("获取聊天历史失败");
        }
    }

    /**
     * 获取最近的会话列表
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<List<String>> getRecentSessions(
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            List<String> sessions = aiChatService.getRecentSessions(userId, limit);
            return Result.success(sessions);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return Result.error("获取会话列表失败");
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/session/new")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Map<String, Object>> createNewSession(
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getCurrentUserId(httpRequest);
            String sessionId = UUID.randomUUID().toString();
            String title = "新对话";
            
            if (request != null && request.get("title") != null) {
                title = (String) request.get("title");
            }
            
            // 保存会话到数据库
            AiChatSession session = new AiChatSession(userId, sessionId, title);
            aiChatSessionMapper.insert(session);
            
            Map<String, Object> result = Map.of(
                "sessionId", sessionId,
                "title", title,
                "id", session.getId()
            );
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建新会话失败", e);
            return Result.error("创建新会话失败");
        }
    }
    
    /**
     * 获取用户的所有会话列表
     */
    @GetMapping("/sessions/list")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<List<AiChatSessionVO>> getSessionList(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            List<AiChatSession> sessions = aiChatSessionMapper.selectByUserId(userId);
            
            List<AiChatSessionVO> sessionVOs = sessions.stream().map(session -> {
                AiChatSessionVO vo = new AiChatSessionVO();
                vo.setId(session.getId());
                vo.setSessionId(session.getSessionId());
                vo.setTitle(session.getTitle());
                vo.setCreateTime(session.getCreateTime());
                vo.setUpdateTime(session.getUpdateTime());
                
                // 获取最后一条消息和消息数量
                try {
                    List<AiChatRecord> records = aiChatService.getChatHistory(userId, session.getSessionId(), 1);
                    if (!records.isEmpty()) {
                        AiChatRecord lastRecord = records.get(0);
                        String content = lastRecord.getContent();
                        if (content.length() > 50) {
                            content = content.substring(0, 50) + "...";
                        }
                        vo.setLastMessage(content);
                    }
                    
                    List<AiChatRecord> allRecords = aiChatService.getChatHistory(userId, session.getSessionId(), 1000);
                    vo.setMessageCount(allRecords.size());
                } catch (Exception e) {
                    log.warn("获取会话消息信息失败: {}", e.getMessage());
                    vo.setLastMessage("");
                    vo.setMessageCount(0);
                }
                
                return vo;
            }).toList();
            
            return Result.success(sessionVOs);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return Result.error("获取会话列表失败");
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Void> deleteSession(
            @PathVariable String sessionId,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            AiChatSession session = aiChatSessionMapper.selectByUserIdAndSessionId(userId, sessionId);
            
            if (session == null) {
                return Result.error("会话不存在");
            }
            
            // 使用MyBatis-Plus的逻辑删除
            aiChatSessionMapper.deleteById(session.getId());
            
            return Result.success();
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return Result.error("删除会话失败");
        }
    }
    
    /**
     * 更新会话标题
     */
    @PutMapping("/session/{sessionId}/title")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Result<Void> updateSessionTitle(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = getCurrentUserId(httpRequest);
            String newTitle = (String) request.get("title");
            
            if (newTitle == null || newTitle.trim().isEmpty()) {
                return Result.error("标题不能为空");
            }
            
            AiChatSession session = aiChatSessionMapper.selectByUserIdAndSessionId(userId, sessionId);
            if (session == null) {
                return Result.error("会话不存在");
            }
            
            session.setTitle(newTitle.trim());
            aiChatSessionMapper.updateById(session);
            
            return Result.success();
        } catch (Exception e) {
            log.error("更新会话标题失败", e);
            return Result.error("更新会话标题失败");
        }
    }

    /**
     * 简单的流式响应测试（不涉及数据库操作）
     */
    @PostMapping(value = "/test/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // 临时注释用于调试
    public Flux<String> testStream(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            // 获取当前安全上下文
            SecurityContext securityContext = SecurityContextHolder.getContext();
            
            String message = (String) request.get("message");
            if (message == null) {
                message = "测试消息";
            }
            
            final String finalMessage = message; // 使变量为final
            
            log.info("开始流式响应测试，消息: {}, 安全上下文: {}", finalMessage, securityContext.getAuthentication());
            
            // 创建简单的流式响应，不涉及数据库操作
            return Flux.range(1, 5)
                    .delayElements(java.time.Duration.ofMillis(500))
                    .map(i -> "data: 测试流式响应 " + i + ": " + finalMessage + "\n\n")
                    .concatWith(Flux.just("data: [DONE]\n\n"))
                    .contextWrite(Context.of(SecurityContext.class, securityContext))
                    .doOnNext(data -> log.info("发送流式数据: {}", data.trim()))
                    .doOnError(error -> log.error("流式响应错误", error))
                    .doOnComplete(() -> log.info("流式响应完成"));

        } catch (Exception e) {
            log.error("流式响应测试失败", e);
            return Flux.just("data: 流式响应测试失败: " + e.getMessage() + "\n\n", "data: [DONE]\n\n");
        }
    }

    /**
     * 从请求中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 优先使用SecurityContextHolder获取用户信息（适用于Spring Security已认证的情况）
        try {
            org.springframework.security.core.Authentication authentication = 
                SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
                
                // 获取用户详情
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    org.springframework.security.core.userdetails.UserDetails userDetails = 
                        (org.springframework.security.core.userdetails.UserDetails) principal;
                    String username = userDetails.getUsername();
                    
                    // 通过用户名查询用户ID
                    com.qltiku2.entity.SysUser user = 
                        sysUserMapper.selectByUsername(username);
                    if (user != null) {
                        return user.getId();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从SecurityContext获取用户信息失败，尝试从token获取", e);
        }
        
        // 如果SecurityContext中没有，则尝试从token中获取（兼容旧逻辑）
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                return jwtUtils.getUserIdFromToken(token);
            } catch (Exception e) {
                log.error("从token获取用户ID失败", e);
            }
        }
        
        // 临时调试：如果无法获取用户信息，返回默认用户ID 1
        log.warn("无法获取用户信息，使用默认用户ID 1 进行调试");
        return 1L;
    }
}