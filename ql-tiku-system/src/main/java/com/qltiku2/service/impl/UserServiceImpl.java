package com.qltiku2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qltiku2.common.Result;
import com.qltiku2.dto.UserQueryRequest;
import com.qltiku2.dto.UserSaveRequest;
import com.qltiku2.entity.AnswerRecord;
import com.qltiku2.entity.SysUser;
import com.qltiku2.mapper.AnswerRecordMapper;
import com.qltiku2.mapper.SysUserMapper;
import com.qltiku2.service.UserService;
import com.qltiku2.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * @author qltiku2
 */
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AnswerRecordMapper answerRecordMapper;
    
    @Override
    public Result<IPage<UserVO>> getUserPage(UserQueryRequest request) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            
            if (StringUtils.hasText(request.getKeyword())) {
                queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getUsername, request.getKeyword())
                    .or()
                    .like(SysUser::getRealName, request.getKeyword())
                );
            }
            
            if (request.getUserType() != null) {
                queryWrapper.eq(SysUser::getUserType, request.getUserType());
            }
            
            if (request.getStatus() != null) {
                queryWrapper.eq(SysUser::getStatus, request.getStatus());
            }
            
            if (StringUtils.hasText(request.getStartTime())) {
                queryWrapper.ge(SysUser::getCreateTime, request.getStartTime());
            }
            
            if (StringUtils.hasText(request.getEndTime())) {
                queryWrapper.le(SysUser::getCreateTime, request.getEndTime());
            }
            
            queryWrapper.eq(SysUser::getDeleted, 0);
            queryWrapper.orderByDesc(SysUser::getCreateTime);
            
            // 分页查询
            Page<SysUser> page = new Page<>(request.getCurrent(), request.getSize());
            IPage<SysUser> userPage = sysUserMapper.selectPage(page, queryWrapper);
            
            // 转换为VO
            List<UserVO> userVOList = userPage.getRecords().stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList());
            
            Page<UserVO> resultPage = new Page<>(request.getCurrent(), request.getSize(), userPage.getTotal());
            resultPage.setRecords(userVOList);
            
            return Result.success(resultPage);
        } catch (Exception e) {
            return Result.error("查询用户列表失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<UserVO> getUserById(Long id) {
        try {
            SysUser user = sysUserMapper.selectById(id);
            if (user == null || user.getDeleted() == 1) {
                return Result.error("用户不存在");
            }
            
            UserVO userVO = convertToUserVO(user);
            return Result.success(userVO);
        } catch (Exception e) {
            return Result.error("获取用户详情失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> createUser(UserSaveRequest request) {
        try {
            // 检查用户名是否已存在
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getUsername, request.getUsername())
                       .eq(SysUser::getDeleted, 0);
            
            if (sysUserMapper.selectCount(queryWrapper) > 0) {
                return Result.error("用户名已存在");
            }
            
            // 创建用户
            SysUser user = new SysUser();
            BeanUtils.copyProperties(request, user);
            
            // 加密密码
            if (StringUtils.hasText(request.getPassword())) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            } else {
                // 默认密码
                user.setPassword(passwordEncoder.encode("123456"));
            }
            
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            user.setDeleted(0);
            
            sysUserMapper.insert(user);
            
            return Result.success("创建用户成功");
        } catch (Exception e) {
            return Result.error("创建用户失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateUser(Long id, UserSaveRequest request) {
        try {
            SysUser existingUser = sysUserMapper.selectById(id);
            if (existingUser == null || existingUser.getDeleted() == 1) {
                return Result.error("用户不存在");
            }
            
            // 检查用户名是否已被其他用户使用
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getUsername, request.getUsername())
                       .ne(SysUser::getId, id)
                       .eq(SysUser::getDeleted, 0);
            
            if (sysUserMapper.selectCount(queryWrapper) > 0) {
                return Result.error("用户名已被其他用户使用");
            }
            
            // 更新用户信息
            SysUser user = new SysUser();
            BeanUtils.copyProperties(request, user);
            user.setId(id);
            
            // 如果提供了新密码，则加密
            if (StringUtils.hasText(request.getPassword())) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            } else {
                user.setPassword(null); // 不更新密码
            }
            
            user.setUpdateTime(LocalDateTime.now());
            
            sysUserMapper.updateById(user);
            
            return Result.success("更新用户成功");
        } catch (Exception e) {
            return Result.error("更新用户失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> deleteUser(Long id) {
        try {
            SysUser user = sysUserMapper.selectById(id);
            if (user == null || user.getDeleted() == 1) {
                return Result.error("用户不存在");
            }
            
            // 软删除
            user.setDeleted(1);
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
            
            return Result.success("删除用户成功");
        } catch (Exception e) {
            return Result.error("删除用户失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> resetPassword(Long id) {
        try {
            SysUser user = sysUserMapper.selectById(id);
            if (user == null || user.getDeleted() == 1) {
                return Result.error("用户不存在");
            }
            
            // 重置为默认密码
            user.setPassword(passwordEncoder.encode("123456"));
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
            
            return Result.success("重置密码成功，新密码为：123456");
        } catch (Exception e) {
            return Result.error("重置密码失败：" + e.getMessage());
        }
    }
    
    @Override
    public Result<String> updateUserStatus(Long id, Integer status) {
        try {
            SysUser user = sysUserMapper.selectById(id);
            if (user == null || user.getDeleted() == 1) {
                return Result.error("用户不存在");
            }
            
            user.setStatus(status);
            user.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(user);
            
            String statusName = status == 1 ? "启用" : "禁用";
            return Result.success(statusName + "用户成功");
        } catch (Exception e) {
            return Result.error("更新用户状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 转换为UserVO
     */
    private UserVO convertToUserVO(SysUser user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        
        // 设置头像字段
        userVO.setAvatar(user.getAvatar());
        
        // 设置用户类型名称
        switch (user.getUserType()) {
            case 0:
                userVO.setUserTypeName("学生");
                break;
            case 1:
                userVO.setUserTypeName("教师");
                break;
            case 2:
                userVO.setUserTypeName("管理员");
                break;
            default:
                userVO.setUserTypeName("未知");
        }
        
        // 设置状态名称
        userVO.setStatusName(user.getStatus() == 1 ? "启用" : "禁用");
        
        // 设置前端el-switch组件期望的状态值
        userVO.setStatusValue(user.getStatus() == 1 ? "active" : "disabled");
        
        // 设置注册时间
        userVO.setRegisterTime(user.getCreateTime());
        
        // 查询练习题数（practiceType = 1 表示练习模式）
        Integer practiceCount = 0;
        try {
            LambdaQueryWrapper<AnswerRecord> practiceWrapper = new LambdaQueryWrapper<>();
            practiceWrapper.eq(AnswerRecord::getUserId, user.getId())
                          .eq(AnswerRecord::getPracticeType, 1);
            Long count = answerRecordMapper.selectCount(practiceWrapper);
            practiceCount = count != null ? count.intValue() : 0;
        } catch (Exception e) {
            practiceCount = 0;
        }
        userVO.setPracticeCount(practiceCount);
        
        // 查询考试次数（practiceType = 2 表示考试模式，统计不同的examId数量）
        // 使用原生SQL查询来统计不同的examId数量
        Integer examCount = 0;
        try {
            examCount = answerRecordMapper.countDistinctExamsByUserId(user.getId());
            if (examCount == null) {
                examCount = 0;
            }
        } catch (Exception e) {
            // 如果查询失败，设置为0
            examCount = 0;
        }
        userVO.setExamCount(examCount);
        
        // 计算正确率
        try {
            LambdaQueryWrapper<AnswerRecord> allAnswersWrapper = new LambdaQueryWrapper<>();
            allAnswersWrapper.eq(AnswerRecord::getUserId, user.getId());
            Long totalAnswers = answerRecordMapper.selectCount(allAnswersWrapper);
            
            if (totalAnswers != null && totalAnswers > 0) {
                LambdaQueryWrapper<AnswerRecord> correctAnswersWrapper = new LambdaQueryWrapper<>();
                correctAnswersWrapper.eq(AnswerRecord::getUserId, user.getId())
                                    .eq(AnswerRecord::getIsCorrect, 1);
                Long correctAnswers = answerRecordMapper.selectCount(correctAnswersWrapper);
                
                if (correctAnswers != null) {
                    double correctRate = (correctAnswers.doubleValue() / totalAnswers.doubleValue()) * 100;
                    userVO.setCorrectRate(Math.round(correctRate * 100.0) / 100.0); // 保留两位小数
                } else {
                    userVO.setCorrectRate(0.0);
                }
            } else {
                userVO.setCorrectRate(0.0);
            }
        } catch (Exception e) {
            userVO.setCorrectRate(0.0);
        }
        
        // 计算学习天数（从第一次答题到现在的天数）
        LambdaQueryWrapper<AnswerRecord> studyDaysWrapper = new LambdaQueryWrapper<>();
        studyDaysWrapper.eq(AnswerRecord::getUserId, user.getId())
                       .orderByAsc(AnswerRecord::getCreateTime);
        java.util.List<AnswerRecord> answerRecords = answerRecordMapper.selectList(studyDaysWrapper);
        
        if (!answerRecords.isEmpty()) {
            AnswerRecord firstAnswer = answerRecords.get(0);
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                firstAnswer.getCreateTime().toLocalDate(), 
                LocalDateTime.now().toLocalDate()
            ) + 1; // 包含当天
            userVO.setStudyDays((int) daysBetween);
        } else {
            userVO.setStudyDays(0);
        }
        
        // 获取最近活动（最近10条答题记录和考试记录）
        java.util.List<UserVO.RecentActivity> recentActivities = new java.util.ArrayList<>();
        
        // 获取最近的答题记录
        LambdaQueryWrapper<AnswerRecord> recentAnswersWrapper = new LambdaQueryWrapper<>();
        recentAnswersWrapper.eq(AnswerRecord::getUserId, user.getId())
                           .orderByDesc(AnswerRecord::getCreateTime);
        
        // 使用分页查询获取最近10条记录
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AnswerRecord> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        com.baomidou.mybatisplus.core.metadata.IPage<AnswerRecord> recentAnswersPage = 
            answerRecordMapper.selectPage(page, recentAnswersWrapper);
        java.util.List<AnswerRecord> recentAnswers = recentAnswersPage.getRecords();
        
        for (AnswerRecord answer : recentAnswers) {
            UserVO.RecentActivity activity = new UserVO.RecentActivity();
            activity.setId(answer.getId());
            
            String activityType = answer.getPracticeType() == 1 ? "练习" : "考试";
            String result = answer.getIsCorrect() == 1 ? "答对" : "答错";
            activity.setDescription(String.format("%s了一道题目，%s", activityType, result));
            
            activity.setTime(answer.getCreateTime().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            ));
            
            recentActivities.add(activity);
        }
        
        userVO.setRecentActivities(recentActivities);
        
        return userVO;
    }
}