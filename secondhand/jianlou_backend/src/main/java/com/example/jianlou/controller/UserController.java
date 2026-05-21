package com.example.jianlou.controller;

import com.example.jianlou.common.EncryptUtil;
import com.example.jianlou.common.SpringContextUtil;
import com.example.jianlou.entity.Cookie;
import com.example.jianlou.entity.User;
import com.example.jianlou.repository.CookieRepository;
import com.example.jianlou.repository.FollowRecordRepository;
import com.example.jianlou.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/user")
@CrossOrigin // 跨域支持
public class UserController {
    @Resource
    private UserRepository userRepository;
    @Resource
    private CookieRepository cookieRepository;
    @Resource
    private EncryptUtil encryptUtil;

    private static final String ROOT_URL = "http://192.168.117.31:8080";

    // 注册接口
    @PostMapping("/register/")
    public Map<String, Object> register(
            @RequestParam String account,
            @RequestParam String password,
            @RequestParam(required = false, defaultValue = "pending") String user_status,
            @RequestParam(required = false, defaultValue = "user") String user_type
    ) {
        Map<String, Object> result = new HashMap<>();
        // 判断账号是否存在
        User existingUser = userRepository.findByAccount(account);
        if (existingUser != null) {
            // 账号已存在：根据状态返回不同提示
            switch (existingUser.getUserStatus()) {
                case "pending":
                    result.put("code", "pending");
                    result.put("msg", "账号已提交审核，请勿重复注册");
                    break;
                case "rejected":
                    result.put("code", "rejected");
                    result.put("msg", "账号审核未通过，无法重复注册");
                    break;
                case "approved":
                    result.put("code", "exists");
                    result.put("msg", "账号已存在，请直接登录");
                    break;
                case "banned": //禁止状态的提示
                    result.put("code", "banned");
                    result.put("msg", "账号已被禁止，无法重复注册");
                    break;
            }
            return result;
        }

        // 账号不存在：正常注册
        String pwd = encryptUtil.saltedPassword(password);
        try {
            User user = new User();
            user.setAccount(account);
            user.setPassword(pwd);
            user.setUsername("捡喽用户" + account);
            user.setUserStatus(user_status);
            user.setUserType(user_type);
            userRepository.save(user);
            result.put("code", "success");
            result.put("msg", "注册申请已提交，等待管理员审核");
        } catch (Exception e) {
            result.put("code", "failed");
            result.put("msg", "注册失败");
        }
        return result;
    }

    // 登录接口
    @PostMapping("/login/")
    public Object login(@RequestParam String account, @RequestParam String password) {
        User user = userRepository.findByAccount(account);
        if (user == null) {
            return "failed"; // 账号不存在
        }

        // 验证密码
        if (!user.getPassword().equals(encryptUtil.saltedPassword(password))) {
            return "failed"; // 密码错误
        }

        // 检查用户状态
        String userStatus = user.getUserStatus();
        switch (userStatus) {
            case "pending":
                return "pending"; // 待审核
            case "rejected":
                return "rejected"; // 审核未通过
            case "banned": // 禁止状态不允许登录
                return "banned";
            case "approved":
                // 生成cookie,基于时间戳 + 账号，确保每次登录生成不同的 Cookie
                String cookie = encryptUtil.md5hex(new Date().toString() + account + new Date().toString());
                Cookie oldCookie = cookieRepository.findByAccount(account);
                if (oldCookie != null) {
                    oldCookie.setCookie(cookie);
                    oldCookie.setChangeTime(new Date());
                    cookieRepository.save(oldCookie);
                } else {
                    Cookie newCookie = new Cookie();
                    newCookie.setCookie(cookie);
                    newCookie.setAccount(account);
                    newCookie.setChangeTime(new Date());
                    cookieRepository.save(newCookie);
                }

                // 构造返回数据
                Map<String, String> res = new HashMap<>();
                res.put("account", account);
                res.put("cookie", cookie);
                res.put("user_name", user.getUsername());
                res.put("user_status", userStatus);
                res.put("user_type", user.getUserType());
                String headUrl = user.getHead() == null || user.getHead().trim().isEmpty()
                        ? ""
                        : ROOT_URL + "/" + user.getHead().trim().replace("\\", "/");
                res.put("user_head", headUrl);
                return res;
            default:
                return "failed";
        }
    }

    @PostMapping("/getUserInfo/")
    public Object getUserInfo(@RequestParam String cookie) {
        try {
            Cookie c = cookieRepository.findByCookie(cookie);
            if (c == null) {
                return "failed";
            }
            User user = userRepository.findByAccount(c.getAccount());
            if (user == null) {
                return "failed";
            }
            // 返回完整用户信息，包含完整头像URL
            Map<String, String> res = new HashMap<>();
            res.put("account", user.getAccount());
            res.put("user_name", user.getUsername());
            res.put("user_type", user.getUserType());
            res.put("user_status", user.getUserStatus());
            String headUrl = user.getHead() == null || user.getHead().trim().isEmpty()
                    ? ""
                    : ROOT_URL + "/" + user.getHead().trim().replace("\\", "/");
            res.put("user_head", headUrl);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }


    //修改昵称、头像、密码
    @PostMapping("/editname/")
    public String editName(@RequestParam String cookie, @RequestParam String user_name) {
        try {
            Cookie c = cookieRepository.findByCookie(cookie);
            User user = userRepository.findByAccount(c.getAccount());
            user.setUsername(user_name);
            userRepository.save(user);
            return "success";
        } catch (Exception e) {
            return "failed";
        }
    }

    @PostMapping("/edithead/")
    public String editHead(@RequestParam String cookie, @RequestParam("image") MultipartFile file) {
        try {
            Cookie c = cookieRepository.findByCookie(cookie);
            String account = c.getAccount();
            String uploadPath = "upload/" + account + "/head/";
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filename = file.getOriginalFilename();
            File dest = new File(uploadPath + filename);
            file.transferTo(dest);
            User user = userRepository.findByAccount(account);
            user.setHead(uploadPath + filename);
            userRepository.save(user);
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    @PostMapping("/changepwd/")
    public String changePwd(@RequestParam String cookie, @RequestParam String old_password, @RequestParam String new_password) {
        try {
            Cookie c = cookieRepository.findByCookie(cookie);
            User user = userRepository.findByAccount(c.getAccount());
            if (user.getPassword().equals(encryptUtil.saltedPassword(old_password))) {
                user.setPassword(encryptUtil.saltedPassword(new_password));
                userRepository.save(user);
                return "success";
            } else {
                return "failed";
            }
        } catch (Exception e) {
            return "failed";
        }
    }

    /**
     * 获取用户统计信息（包括点赞数、粉丝数、关注数）
     */
    @PostMapping("/stats/")
    public Map<String, Object> getUserStats(
            @RequestParam String cookie,
            @RequestParam(required = false) String targetAccount) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 验证当前用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            String currentAccount = cookieEntity.getAccount();

            // 如果指定了目标账号，查询目标用户；否则查询当前用户
            String queryAccount = (targetAccount != null && !targetAccount.isEmpty())
                    ? targetAccount : currentAccount;

            User user = userRepository.findByAccount(queryAccount);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 获取统计信息
            result.put("success", true);
            result.put("account", user.getAccount());
            result.put("username", user.getUsername());
            result.put("likes_received", user.getLikesReceived()); // 收到的赞数
            result.put("followers_count", user.getFollowersCount()); // 粉丝数
            result.put("following_count", user.getFollowingCount()); // 关注数

            // 如果是查询其他用户，检查是否已关注
            if (!queryAccount.equals(currentAccount)) {
                FollowRecordRepository followRepo = SpringContextUtil.getBean(FollowRecordRepository.class);
                boolean isFollowing = followRepo.existsByFollowerAccountAndFollowingAccount(
                        currentAccount, queryAccount);
                result.put("is_following", isFollowing);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "查询失败");
        }

        return result;
    }
}