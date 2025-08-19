package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qltiku2.common.Result;
import com.qltiku2.entity.AiChatRecord;
import com.qltiku2.entity.AiChatSession;
import com.qltiku2.entity.AiGradingRecord;
import com.qltiku2.entity.SysUser;
import com.qltiku2.service.AiChatRecordService;
import com.qltiku2.service.AiChatSessionService;
import com.qltiku2.service.AiGradingRecordService;
import com.qltiku2.mapper.SysUserMapper;
import com.qltiku2.vo.AiChatRecordVO;
import com.qltiku2.vo.AiChatSessionAdminVO;
import com.qltiku2.vo.AiGradingRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI数据管理控制器
 * 提供AI聊天会话、聊天记录、评分记录的查询和删除功能
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
@Tag(name = "AI数据管理", description = "AI数据管理相关接口")
public class AdminAiDataController {

    private final AiChatSessionService aiChatSessionService;
    private final AiChatRecordService aiChatRecordService;
    private final AiGradingRecordService aiGradingRecordService;
    private final SysUserMapper sysUserMapper;

    /**
     * 获取AI聊天会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取AI聊天会话列表")
    public Result<IPage<AiChatSessionAdminVO>> getSessions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        
        try {
            Page<AiChatSession> pageParam = new Page<>(page, size);
            QueryWrapper<AiChatSession> queryWrapper = new QueryWrapper<>();
            
            // 只查询未删除的记录
            queryWrapper.eq("deleted", 0);
            
            // 关键词搜索
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .eq("user_id", keyword)
                    .or()
                    .like("session_id", keyword)
                );
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc("create_time");
            
            IPage<AiChatSession> sessionPage = aiChatSessionService.page(pageParam, queryWrapper);
            
            // 转换为VO对象
            Page<AiChatSessionAdminVO> voPage = new Page<>(page, size, sessionPage.getTotal());
            List<AiChatSessionAdminVO> voList = sessionPage.getRecords().stream().map(session -> {
                AiChatSessionAdminVO vo = new AiChatSessionAdminVO();
                vo.setId(session.getId());
                vo.setUserId(session.getUserId());
                vo.setSessionId(session.getSessionId());
                vo.setTitle(session.getTitle());
                vo.setCreateTime(session.getCreateTime());
                vo.setUpdateTime(session.getUpdateTime());
                
                // 获取用户信息
                if (session.getUserId() != null) {
                    SysUser user = sysUserMapper.selectById(session.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setNickname(user.getNickname());
                    }
                }
                
                // 获取消息数量
                try {
                    QueryWrapper<AiChatRecord> recordQuery = new QueryWrapper<>();
                    recordQuery.eq("session_id", session.getSessionId());
                    long count = aiChatRecordService.count(recordQuery);
                    vo.setMessageCount((int) count);
                } catch (Exception e) {
                    vo.setMessageCount(0);
                }
                
                return vo;
            }).toList();
            
            voPage.setRecords(voList);
            return Result.success(voPage);
            
        } catch (Exception e) {
            log.error("获取AI聊天会话列表失败", e);
            return Result.error("获取AI聊天会话列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取AI聊天记录列表
     */
    @GetMapping("/chat-records")
    @Operation(summary = "获取AI聊天记录列表")
    public Result<IPage<AiChatRecordVO>> getChatRecords(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "消息类型") @RequestParam(required = false) String messageType) {
        
        try {
            Page<AiChatRecord> pageParam = new Page<>(page, size);
            QueryWrapper<AiChatRecord> queryWrapper = new QueryWrapper<>();
            
            // 关键词搜索
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper -> wrapper
                    .eq("user_id", keyword)
                    .or()
                    .like("session_id", keyword)
                    .or()
                    .eq("question_id", keyword)
                );
            }
            
            // 消息类型过滤 - 转换前端字符串为数据库数字
            if (StringUtils.hasText(messageType)) {
                if ("user".equals(messageType)) {
                    queryWrapper.eq("message_type", AiChatRecord.MESSAGE_TYPE_USER);
                } else if ("assistant".equals(messageType)) {
                    queryWrapper.eq("message_type", AiChatRecord.MESSAGE_TYPE_AI);
                }
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc("create_time");
            
            IPage<AiChatRecord> recordPage = aiChatRecordService.page(pageParam, queryWrapper);
            
            // 转换为VO对象
            Page<AiChatRecordVO> voPage = new Page<>(page, size, recordPage.getTotal());
            List<AiChatRecordVO> voList = recordPage.getRecords().stream().map(record -> {
                AiChatRecordVO vo = new AiChatRecordVO();
                vo.setId(record.getId());
                vo.setUserId(record.getUserId());
                vo.setSessionId(record.getSessionId());
                vo.setQuestionId(record.getQuestionId());
                vo.setContent(record.getContent());
                vo.setCreateTime(record.getCreateTime());
                
                // 转换消息类型为前端期望的字符串
                if (record.getMessageType() != null) {
                    if (record.getMessageType().equals(AiChatRecord.MESSAGE_TYPE_USER)) {
                        vo.setMessageType("user");
                    } else if (record.getMessageType().equals(AiChatRecord.MESSAGE_TYPE_AI)) {
                        vo.setMessageType("assistant");
                    }
                }
                
                // 获取用户信息
                if (record.getUserId() != null) {
                    SysUser user = sysUserMapper.selectById(record.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setNickname(user.getNickname());
                    }
                }
                
                return vo;
            }).toList();
            
            voPage.setRecords(voList);
            return Result.success(voPage);
            
        } catch (Exception e) {
            log.error("获取AI聊天记录列表失败", e);
            return Result.error("获取AI聊天记录列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取AI评分记录列表
     */
    @GetMapping("/grading-records")
    @Operation(summary = "获取AI评分记录列表")
    public Result<IPage<AiGradingRecordVO>> getGradingRecords(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否正确") @RequestParam(required = false) Integer isCorrect) {
        
        try {
            Page<AiGradingRecord> pageParam = new Page<>(page, size);
            QueryWrapper<AiGradingRecord> queryWrapper = new QueryWrapper<>();
            
            // 只查询未删除的记录
            queryWrapper.eq("is_deleted", 0);
            
            // 关键词搜索
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper -> wrapper
                    .eq("user_id", keyword)
                    .or()
                    .eq("question_id", keyword)
                );
            }
            
            // 是否正确过滤
            if (isCorrect != null) {
                queryWrapper.eq("is_correct", isCorrect);
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc("create_time");
            
            IPage<AiGradingRecord> gradingPage = aiGradingRecordService.page(pageParam, queryWrapper);
            
            // 转换为VO对象
            Page<AiGradingRecordVO> voPage = new Page<>(page, size, gradingPage.getTotal());
            List<AiGradingRecordVO> voList = gradingPage.getRecords().stream().map(record -> {
                AiGradingRecordVO vo = new AiGradingRecordVO();
                vo.setId(record.getId());
                vo.setUserId(record.getUserId());
                vo.setQuestionId(record.getQuestionId());
                vo.setUserAnswer(record.getUserAnswer());
                vo.setAiResult(record.getAiResult());
                vo.setIsCorrect(record.getIsCorrect());
                vo.setCreateTime(record.getCreateTime());
                vo.setUpdateTime(record.getUpdateTime());
                
                // 获取用户信息
                if (record.getUserId() != null) {
                    SysUser user = sysUserMapper.selectById(record.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setNickname(user.getNickname());
                    }
                }
                
                return vo;
            }).toList();
            
            voPage.setRecords(voList);
            return Result.success(voPage);
            
        } catch (Exception e) {
            log.error("获取AI评分记录列表失败", e);
            return Result.error("获取AI评分记录列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取会话的聊天记录
     */
    @GetMapping("/sessions/{sessionId}/records")
    @Operation(summary = "获取会话的聊天记录")
    public Result<List<AiChatRecordVO>> getSessionRecords(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        try {
            QueryWrapper<AiChatRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("session_id", sessionId);
            queryWrapper.orderByAsc("create_time");
            
            List<AiChatRecord> records = aiChatRecordService.list(queryWrapper);
            
            // 转换为VO对象
            List<AiChatRecordVO> voList = records.stream().map(record -> {
                AiChatRecordVO vo = new AiChatRecordVO();
                vo.setId(record.getId());
                vo.setUserId(record.getUserId());
                vo.setSessionId(record.getSessionId());
                vo.setQuestionId(record.getQuestionId());
                vo.setContent(record.getContent());
                vo.setCreateTime(record.getCreateTime());
                
                // 转换消息类型为前端期望的字符串
                if (record.getMessageType() != null) {
                    if (record.getMessageType().equals(AiChatRecord.MESSAGE_TYPE_USER)) {
                        vo.setMessageType("user");
                    } else if (record.getMessageType().equals(AiChatRecord.MESSAGE_TYPE_AI)) {
                        vo.setMessageType("assistant");
                    }
                }
                
                // 获取用户信息
                if (record.getUserId() != null) {
                    SysUser user = sysUserMapper.selectById(record.getUserId());
                    if (user != null) {
                        vo.setUsername(user.getUsername());
                        vo.setNickname(user.getNickname());
                    }
                }
                
                return vo;
            }).toList();
            
            return Result.success(voList);
            
        } catch (Exception e) {
            log.error("获取会话聊天记录失败", e);
            return Result.error("获取会话聊天记录失败：" + e.getMessage());
        }
    }

    /**
     * 删除AI聊天会话
     */
    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "删除AI聊天会话")
    public Result<Void> deleteSession(@Parameter(description = "会话ID") @PathVariable Long id) {
        try {
            // 软删除会话
            AiChatSession session = new AiChatSession();
            session.setId(id);
            session.setDeleted(1);
            aiChatSessionService.updateById(session);
            
            // 同时删除该会话的所有聊天记录
            AiChatSession existSession = aiChatSessionService.getById(id);
            if (existSession != null) {
                QueryWrapper<AiChatRecord> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("session_id", existSession.getSessionId());
                aiChatRecordService.remove(queryWrapper);
            }
            
            return Result.success();
            
        } catch (Exception e) {
            log.error("删除AI聊天会话失败", e);
            return Result.error("删除AI聊天会话失败：" + e.getMessage());
        }
    }

    /**
     * 删除AI聊天记录
     */
    @DeleteMapping("/chat-records/{id}")
    @Operation(summary = "删除AI聊天记录")
    public Result<Void> deleteChatRecord(@Parameter(description = "记录ID") @PathVariable Long id) {
        try {
            aiChatRecordService.removeById(id);
            return Result.success();
            
        } catch (Exception e) {
            log.error("删除AI聊天记录失败", e);
            return Result.error("删除AI聊天记录失败：" + e.getMessage());
        }
    }

    /**
     * 删除AI评分记录
     */
    @DeleteMapping("/grading-records/{id}")
    @Operation(summary = "删除AI评分记录")
    public Result<Void> deleteGradingRecord(@Parameter(description = "记录ID") @PathVariable Long id) {
        try {
            // 软删除评分记录
            AiGradingRecord record = new AiGradingRecord();
            record.setId(id);
            record.setIsDeleted(true);
            aiGradingRecordService.updateById(record);
            
            return Result.success();
            
        } catch (Exception e) {
            log.error("删除AI评分记录失败", e);
            return Result.error("删除AI评分记录失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除AI聊天会话
     */
    @DeleteMapping("/sessions/batch")
    @Operation(summary = "批量删除AI聊天会话")
    public Result<Void> batchDeleteSessions(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的记录");
            }
            
            // 批量软删除会话
            for (Long id : ids) {
                AiChatSession session = new AiChatSession();
                session.setId(id);
                session.setDeleted(1);
                aiChatSessionService.updateById(session);
                
                // 同时删除该会话的所有聊天记录
                AiChatSession existSession = aiChatSessionService.getById(id);
                if (existSession != null) {
                    QueryWrapper<AiChatRecord> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("session_id", existSession.getSessionId());
                    aiChatRecordService.remove(queryWrapper);
                }
            }
            
            return Result.success();
            
        } catch (Exception e) {
            log.error("批量删除AI聊天会话失败", e);
            return Result.error("批量删除AI聊天会话失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除AI聊天记录
     */
    @DeleteMapping("/chat-records/batch")
    @Operation(summary = "批量删除AI聊天记录")
    public Result<Void> batchDeleteChatRecords(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的记录");
            }
            
            aiChatRecordService.removeByIds(ids);
            return Result.success();
            
        } catch (Exception e) {
            log.error("批量删除AI聊天记录失败", e);
            return Result.error("批量删除AI聊天记录失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除AI评分记录
     */
    @DeleteMapping("/grading-records/batch")
    @Operation(summary = "批量删除AI评分记录")
    public Result<Void> batchDeleteGradingRecords(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的记录");
            }
            
            // 批量软删除评分记录
            for (Long id : ids) {
                AiGradingRecord record = new AiGradingRecord();
                record.setId(id);
                record.setIsDeleted(true);
                aiGradingRecordService.updateById(record);
            }
            
            return Result.success();
            
        } catch (Exception e) {
            log.error("批量删除AI评分记录失败", e);
            return Result.error("批量删除AI评分记录失败：" + e.getMessage());
        }
    }
}