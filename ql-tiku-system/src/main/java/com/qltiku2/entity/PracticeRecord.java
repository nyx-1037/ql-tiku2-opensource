package com.qltiku2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 练习记录实体类
 */
@Data
@TableName("practice_records")
public class PracticeRecord {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 科目ID
     */
    @TableField("subject_id")
    private Long subjectId;
    
    /**
     * 科目名称
     */
    @TableField("subject_name")
    private String subjectName;
    
    /**
     * 题目类型（1-单选题，2-多选题，3-判断题，4-填空题，5-大题）
     */
    @TableField("question_type")
    private Integer questionType;
    
    /**
     * 难度（1-简单，2-中等，3-困难）
     */
    @TableField("difficulty")
    private Integer difficulty;
    
    /**
     * 题目数量
     */
    @TableField("total_questions")
    private Integer totalQuestions;
    
    /**
     * 正确数量
     */
    @TableField("correct_count")
    private Integer correctCount;
    
    /**
     * 错误数量
     */
    @TableField("wrong_count")
    private Integer wrongCount;
    
    /**
     * 未答题数量
     */
    @TableField("unanswered_count")
    private Integer unansweredCount;
    
    /**
     * 正确率
     */
    @TableField("accuracy_rate")
    private Double accuracyRate;
    
    /**
     * 总用时（秒）
     */
    @TableField("duration")
    private Integer duration;
    
    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;
    
    /**
     * 状态（1-进行中，2-已完成）
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}