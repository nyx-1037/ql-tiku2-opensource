package com.qltiku2.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qltiku2.common.Result;
import com.qltiku2.dto.AiModelSaveRequest;
import com.qltiku2.service.AiModelService;
import com.qltiku2.vo.AiModelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI模型配置控制器（管理端）
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai-models")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiModelController {

    @Autowired
    private AiModelService aiModelService;

    /**
     * 分页查询AI模型列表
     */
    @GetMapping("/page")
    public Result<IPage<AiModelVO>> getModelPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return aiModelService.getModelPage(current, size, keyword, enabled);
    }

    /**
     * 获取所有模型列表（包括禁用的）
     */
    @GetMapping
    public Result<IPage<AiModelVO>> getAllModels(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean enabled) {
        return aiModelService.getModelPage(current, size, name, enabled);
    }

    /**
     * 根据ID获取模型详情
     */
    @GetMapping("/{id}")
    public Result<AiModelVO> getModelById(@PathVariable Long id) {
        return aiModelService.getModelById(id);
    }

    /**
     * 创建AI模型
     */
    @PostMapping
    public Result<String> createModel(@Valid @RequestBody AiModelSaveRequest request) {
        return aiModelService.createModel(request);
    }

    /**
     * 更新AI模型
     */
    @PutMapping("/{id}")
    public Result<String> updateModel(@PathVariable Long id, 
                                     @Valid @RequestBody AiModelSaveRequest request) {
        return aiModelService.updateModel(id, request);
    }

    /**
     * 删除AI模型
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteModel(@PathVariable Long id) {
        return aiModelService.deleteModel(id);
    }

    /**
     * 批量删除AI模型
     */
    @DeleteMapping("/batch")
    public Result<String> batchDeleteModels(@RequestBody List<Long> ids) {
        return aiModelService.batchDeleteModels(ids);
    }

    /**
     * 启用/禁用AI模型
     */
    @PutMapping("/{id}/status")
    public Result<String> toggleModelStatus(@PathVariable Long id, 
                                           @RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            return Result.error("启用状态不能为空");
        }
        return aiModelService.toggleModelStatus(id, enabled);
    }
}