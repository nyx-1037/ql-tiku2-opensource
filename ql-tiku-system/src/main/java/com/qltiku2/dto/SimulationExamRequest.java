package com.qltiku2.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 模拟考试请求DTO
 * 
 * @author qltiku2
 */
public class SimulationExamRequest {
    
    /**
     * 科目ID
     */
    @NotNull(message = "科目ID不能为空")
    private Long subjectId;
    
    /**
     * 题目数量
     */
    @NotNull(message = "题目数量不能为空")
    @Positive(message = "题目数量必须大于0")
    private Integer questionCount;
    
    /**
     * 考试时长（分钟）
     */
    @NotNull(message = "考试时长不能为空")
    @Positive(message = "考试时长必须大于0")
    private Integer duration;
    
    /**
     * 难度级别列表
     */
    @Size(min = 1, message = "至少选择一个难度级别")
    private List<String> difficulties;
    
    public SimulationExamRequest() {}
    
    public Long getSubjectId() {
        return subjectId;
    }
    
    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public List<String> getDifficulties() {
        return difficulties;
    }
    
    public void setDifficulties(List<String> difficulties) {
        this.difficulties = difficulties;
    }
    
    @Override
    public String toString() {
        return "SimulationExamRequest{" +
                "subjectId=" + subjectId +
                ", questionCount=" + questionCount +
                ", duration=" + duration +
                ", difficulties=" + difficulties +
                '}';
    }
}