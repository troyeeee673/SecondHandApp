package com.example.jianlou.controller;

import com.example.jianlou.entity.*;
import com.example.jianlou.repository.*;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/follow")
@CrossOrigin
public class FollowController {
    @Resource
    private FollowRecordRepository followRecordRepository;

    @Resource
    private CookieRepository cookieRepository;

    @Resource
    private UserRepository userRepository;

    /**
     * 关注/取消关注接口
     */
    @PostMapping("/toggle/")
    public Map<String, Object> toggleFollow(
            @RequestParam String cookie,
            @RequestParam String targetAccount) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 验证当前用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            String followerAccount = cookieEntity.getAccount();
            User follower = userRepository.findByAccount(followerAccount);
            if (follower == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 验证被关注用户
            User following = userRepository.findByAccount(targetAccount);
            if (following == null) {
                result.put("success", false);
                result.put("message", "被关注用户不存在");
                return result;
            }

            // 检查是否关注自己
            if (followerAccount.equals(targetAccount)) {
                result.put("success", false);
                result.put("message", "不能关注自己");
                return result;
            }

            // 检查是否已经关注
            boolean isFollowing = followRecordRepository.existsByFollowerAccountAndFollowingAccount(
                    followerAccount, targetAccount);

            if (isFollowing) {
                // 取消关注
                followRecordRepository.findByFollowerAccountAndFollowingAccount(followerAccount, targetAccount)
                        .ifPresent(followRecordRepository::delete);

                // 更新关注者的关注数
                follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));
                userRepository.save(follower);

                // 更新被关注者的粉丝数
                following.setFollowersCount(Math.max(0, following.getFollowersCount() - 1));
                userRepository.save(following);

                result.put("success", true);
                result.put("message", "已取消关注");
                result.put("following", false);
            } else {
                // 关注
                FollowRecord followRecord = new FollowRecord();
                followRecord.setFollower(follower);
                followRecord.setFollowing(following);
                followRecord.setUniqueKey(followerAccount + "_" + targetAccount);
                followRecordRepository.save(followRecord);

                // 更新关注者的关注数
                follower.setFollowingCount(follower.getFollowingCount() + 1);
                userRepository.save(follower);

                // 更新被关注者的粉丝数
                following.setFollowersCount(following.getFollowersCount() + 1);
                userRepository.save(following);

                result.put("success", true);
                result.put("message", "关注成功");
                result.put("following", true);
            }

            // 返回最新的粉丝数和关注数
            result.put("followers_count", following.getFollowersCount());
            result.put("following_count", follower.getFollowingCount());

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取关注状态接口
     */
    @PostMapping("/status/")
    public Map<String, Object> getFollowStatus(
            @RequestParam String cookie,
            @RequestParam String targetAccount) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 验证当前用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            String followerAccount = cookieEntity.getAccount();

            // 验证被关注用户
            User following = userRepository.findByAccount(targetAccount);
            if (following == null) {
                result.put("success", false);
                result.put("message", "被关注用户不存在");
                return result;
            }

            // 检查是否已经关注
            boolean isFollowing = followRecordRepository.existsByFollowerAccountAndFollowingAccount(
                    followerAccount, targetAccount);

            result.put("success", true);
            result.put("following", isFollowing);
            result.put("followers_count", following.getFollowersCount());

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "查询失败");
        }

        return result;
    }


    /**
     * 获取粉丝列表 - 添加详细日志
     */
    @RequestMapping(value = "/followers/", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> getFollowers(
            @RequestParam String cookie) {

        System.out.println("======= 开始处理 /followers/ 请求 =======");
        System.out.println("接收到的cookie: " + cookie);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证cookie
            System.out.println("1. 开始验证cookie...");
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);

            if (cookieEntity == null) {
                System.out.println("Cookie验证失败：未找到对应记录");
                result.put("success", false);
                result.put("message", "用户未登录");
                System.out.println("返回结果: " + result);
                return result;
            }

            String userAccount = cookieEntity.getAccount();
            System.out.println("2. Cookie验证成功，用户账户: " + userAccount);

            // 2. 查询粉丝列表
            System.out.println("3. 开始查询粉丝列表...");
            List<FollowRecord> followers = followRecordRepository.findByFollowingAccount(userAccount);
            System.out.println("查询到 " + followers.size() + " 个粉丝");

            // 3. 构建返回数据
            List<Map<String, Object>> followerList = new ArrayList<>();

            for (FollowRecord record : followers) {
                System.out.println("处理粉丝记录: " + record);

                if (record.getFollower() == null) {
                    System.out.println("警告：粉丝记录中follower为null");
                    continue;
                }

                Map<String, Object> followerMap = new HashMap<>();
                followerMap.put("account", record.getFollower().getAccount());
                followerMap.put("username", record.getFollower().getUsername() != null ?
                        record.getFollower().getUsername() : "用户");
                followerMap.put("avatar", record.getFollower().getAvatar() != null ?
                        record.getFollower().getAvatar() : "");

                // 检查是否相互关注
                boolean isFollowing = followRecordRepository.existsByFollowerAccountAndFollowingAccount(
                        userAccount, record.getFollower().getAccount());
                followerMap.put("is_following", isFollowing);

                followerList.add(followerMap);
            }

            result.put("success", true);
            result.put("data", followerList);  // 使用"data"字段，符合前端期望
            result.put("count", followerList.size());

            System.out.println("4. 数据处理完成，返回结果: " + result);

        } catch (Exception e) {
            System.err.println("======= 处理过程中发生异常 =======");
            e.printStackTrace();
            System.err.println("异常信息: " + e.getMessage());

            result.put("success", false);
            result.put("message", "服务器内部错误: " + e.getMessage());
        }

        System.out.println("======= 请求处理结束 =======");
        return result;
    }

    /**
     * 获取关注列表 - 添加详细日志
     */
    @RequestMapping(value = "/following/", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> getFollowing(
            @RequestParam String cookie) {

        System.out.println("======= 开始处理 /following/ 请求 =======");
        System.out.println("接收到的cookie: " + cookie);

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证cookie
            System.out.println("1. 开始验证cookie...");
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);

            if (cookieEntity == null) {
                System.out.println("Cookie验证失败：未找到对应记录");
                result.put("success", false);
                result.put("message", "用户未登录");
                System.out.println("返回结果: " + result);
                return result;
            }

            String userAccount = cookieEntity.getAccount();
            System.out.println("2. Cookie验证成功，用户账户: " + userAccount);

            // 2. 查询关注列表
            System.out.println("3. 开始查询关注列表...");
            List<FollowRecord> following = followRecordRepository.findByFollowerAccount(userAccount);
            System.out.println("查询到 " + following.size() + " 个关注");

            // 3. 构建返回数据
            List<Map<String, Object>> followingList = new ArrayList<>();

            for (FollowRecord record : following) {
                System.out.println("处理关注记录: " + record);

                if (record.getFollowing() == null) {
                    System.out.println("警告：关注记录中following为null");
                    continue;
                }

                Map<String, Object> followingMap = new HashMap<>();
                followingMap.put("account", record.getFollowing().getAccount());
                followingMap.put("username", record.getFollowing().getUsername() != null ?
                        record.getFollowing().getUsername() : "用户");
                followingMap.put("avatar", record.getFollowing().getAvatar() != null ?
                        record.getFollowing().getAvatar() : "");
                followingMap.put("is_following", true); // 关注列表中的人都是已关注的

                followingList.add(followingMap);
            }

            result.put("success", true);
            result.put("data", followingList);  // 使用"data"字段，符合前端期望
            result.put("count", followingList.size());

            System.out.println("4. 数据处理完成，返回结果: " + result);

        } catch (Exception e) {
            System.err.println("======= 处理过程中发生异常 =======");
            e.printStackTrace();
            System.err.println("异常信息: " + e.getMessage());

            result.put("success", false);
            result.put("message", "服务器内部错误: " + e.getMessage());
        }

        System.out.println("======= 请求处理结束 =======");
        return result;
    }





}