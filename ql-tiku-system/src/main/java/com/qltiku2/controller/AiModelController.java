package com.qltiku2.controller;

import com.qltiku2.common.Result;
import com.qltiku2.service.AiModelService;
import com.qltiku2.vo.AiModelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI模型配置控制器（客户端）
 *
 * @author qltiku2
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/ai-models")
@CrossOrigin(origins = "*")
public class AiModelController {

    @Autowired
    private AiModelService aiModelService;

    /**
     * 获取所有启用的模型列表（公开接口）
     */
    @GetMapping("/enabled")
    public Result<List<AiModelVO>> getEnabledModels() {
        return aiModelService.getEnabledModels();
    }

    /**
     * 根据代码获取模型详情（公开接口）
     */
    @GetMapping("/code/{code}")
    public Result<AiModelVO> getModelByCode(@PathVariable String code) {
        return aiModelService.getModelByCode(code);
    }
}
