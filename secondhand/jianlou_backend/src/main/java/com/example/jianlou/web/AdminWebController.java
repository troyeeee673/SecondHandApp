package com.example.jianlou.web;

import com.example.jianlou.entity.Goods;
import com.example.jianlou.entity.User;
import com.example.jianlou.common.EncryptUtil;
import com.example.jianlou.repository.GoodsRepository;
import com.example.jianlou.repository.UserRepository;
import com.example.jianlou.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/web")
public class AdminWebController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptUtil encryptUtil;

    //登录
    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String account,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        User admin = adminService.checkAdminLogin(account, password);

        if (admin == null) {
            System.out.println("登录失败：账号或密码错误，或非管理员账号");
            model.addAttribute("error", "账号或密码错误，或非管理员账号");
            return "admin/login"; //页面渲染 + 传递 Model 数据（错误提示）
        }

        System.out.println("登录成功！管理员账号: " + admin.getAccount());
        session.setAttribute("loginAdmin", admin);
        return "redirect:/admin/web/dashboard"; //接口跳转（如登录成功跳后台首页，新的请求）
    }

    //退出登录
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("loginAdmin");
        return "redirect:/admin/web/login";
    }

    //  数据大屏
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        User admin = (User) session.getAttribute("loginAdmin");
        model.addAttribute("admin", admin);

        Map<String, Long> goodsClassifyStat = adminService.countGoodsByClassify();//获取每个类别以及对应数量
        Map<String, Long> userTypeStat = adminService.countUserByType();

        model.addAttribute("goodsClassifyStat", goodsClassifyStat);
        model.addAttribute("userTypeStat", userTypeStat);

        return "admin/dashboard";
    }

    // ====================== 用户管理 ======================
    @GetMapping("/user-manage")
    public String userManage(HttpSession session,
                             @RequestParam(defaultValue = "all") String status,
                             Model model) {
        if (!checkAdminLogin(session)) {  //必须是登录+管理员类型
            return "redirect:/admin/web/login";
        }

        User admin = (User) session.getAttribute("loginAdmin");
        if (admin == null) {
            return "redirect:/admin/web/login";
        }

        model.addAttribute("admin", admin);
        model.addAttribute("activeStatus", status);

        List<User> userList = adminService.getAllUsers();//获取全部用户
        model.addAttribute("userList", userList);

        long pendingCount = userList.stream()
                .filter(user -> "pending".equals(user.getUserStatus()))
                .count();
        long approvedCount = userList.stream()
                .filter(user -> "approved".equals(user.getUserStatus()))
                .count();
        long rejectedCount = userList.stream()
                .filter(user -> "rejected".equals(user.getUserStatus()))
                .count();
        long bannedCount = userList.stream()
                .filter(user -> "banned".equals(user.getUserStatus()))
                .count();

        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("bannedCount", bannedCount);

        if (!"all".equals(status)) {
            userList = userList.stream()
                    .filter(user -> status.equals(user.getUserStatus()))
                    .collect(Collectors.toList());
            model.addAttribute("userList", userList);
        }

        return "admin/user-manage";
    }

    //用户状态更新（user表数据库变化）
    @GetMapping("/user/updateStatus")
    public String updateUserStatus(@RequestParam String account,
                                   @RequestParam String status,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        try {
            adminService.updateUserStatus(account, status);
            redirectAttributes.addFlashAttribute("success", "用户状态已更新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新失败: " + e.getMessage());
        }
        return "redirect:/admin/web/user-manage";
    }

    // ====================== 商品管理 ======================
    @GetMapping("/goods-manage")
    public String goodsManage(@RequestParam(value = "status", defaultValue = "all") String status,
                              @RequestParam(value = "classify", required = false) String classify,
                              @RequestParam(value = "owner", required = false) String owner,
                              @RequestParam(value = "keyword", required = false) String keyword,
                              Model model,
                              HttpSession session) {

        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        User admin = (User) session.getAttribute("loginAdmin");
        model.addAttribute("admin", admin);
        model.addAttribute("currentPage", "goods-manage");

        List<Goods> goodsList;

        if (owner != null && !owner.isEmpty()) {
            goodsList = goodsRepository.findByOwnerAccountOrderByEditDateDesc(owner);
            model.addAttribute("selectedOwner", owner);
        } else if (classify != null && !classify.isEmpty()) {
            if ("all".equals(status)) {
                goodsList = goodsRepository.findByClassifyContainingOrderByEditDateDesc(classify);
            } else {
                goodsList = goodsRepository.findByAuditStatusAndClassifyContainingOrderByEditDateDesc(status, classify);
            }
            model.addAttribute("selectedClassify", classify);
        } else if (keyword != null && !keyword.isEmpty()) {
            if ("all".equals(status)) {
                goodsList = goodsRepository.findByContentContainingOrderByEditDateDesc(keyword);
            } else {
                goodsList = goodsRepository.findByAuditStatusAndContentContainingOrderByEditDateDesc(status, keyword);
            }
            model.addAttribute("searchKeyword", keyword);
        } else {
            if ("all".equals(status)) {
                goodsList = goodsRepository.findAllByOrderByEditDateDesc();
            } else {
                goodsList = goodsRepository.findByAuditStatusOrderByEditDateDesc(status);
            }
        }

        List<String> classifyList = goodsRepository.findAll().stream()
                .map(Goods::getClassify)
                .distinct()
                .collect(Collectors.toList());

        Long pendingCount = goodsRepository.countByAuditStatus("pending");
        Long approvedCount = goodsRepository.countByAuditStatus("approved");
        Long rejectedCount = goodsRepository.countByAuditStatus("rejected");
        Long totalCount = goodsRepository.count();

        model.addAttribute("goodsList", goodsList);
        model.addAttribute("classifyList", classifyList);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeStatus", status);

        return "admin/goods-manage";
    }

    // 审核通过商品
    @GetMapping("/goods/approve")
    public String approveGoods(@RequestParam String hash,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        try {
            goodsRepository.updateAuditStatus(hash, "approved");
            redirectAttributes.addFlashAttribute("success", "商品已审核通过");

            Goods goods = goodsRepository.findById(hash).orElse(null);
            if (goods != null && goods.getOwner() != null) {
                System.out.println("发送审核通过消息给用户: " + goods.getOwner().getAccount());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "审核操作失败: " + e.getMessage());
        }
        return "redirect:/admin/web/goods-manage";
    }

    // 拒绝商品
    @PostMapping("/goods/reject")
    public String rejectGoods(@RequestParam String hash,
                              @RequestParam String reason,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        try {
            if (reason == null || reason.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "请填写拒绝理由");
                return "redirect:/admin/web/goods-manage";
            }

            goodsRepository.updateAuditStatusWithReason(hash, "rejected", reason);
            redirectAttributes.addFlashAttribute("success", "商品已拒绝");

            Goods goods = goodsRepository.findById(hash).orElse(null);
            if (goods != null && goods.getOwner() != null) {
                System.out.println("发送拒绝消息给用户: " + goods.getOwner().getAccount() + ", 理由: " + reason);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "拒绝操作失败: " + e.getMessage());
        }
        return "redirect:/admin/web/goods-manage";
    }

    // ====================== 个人中心 ======================
    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        User admin = (User) session.getAttribute("loginAdmin");
        model.addAttribute("admin", admin);

        return "admin/profile";
    }

    @PostMapping("/updateProfile")
    public String updateProfile(@RequestParam String username,
                                @RequestParam String sex,
                                @RequestParam(required = false) String birthday,
                                @RequestParam(required = false) String location,
                                @RequestParam(required = false) String school,
                                @RequestParam(required = false) String introduction,
                                HttpSession session,
                                Model model) {
        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        User admin = (User) session.getAttribute("loginAdmin");

        try {
            admin.setUsername(username);
            admin.setSex(sex);

            if (birthday != null && !birthday.isEmpty()) {
                try {
                    admin.setBirthday(java.sql.Date.valueOf(birthday));
                } catch (Exception e) {
                    System.out.println("生日格式错误: " + e.getMessage());
                }
            }

            if (location != null) admin.setLocation(location);
            if (school != null) admin.setSchool(school);
            if (introduction != null) admin.setIntroduction(introduction);

            userRepository.save(admin);
            session.setAttribute("loginAdmin", admin);
            model.addAttribute("success", "个人信息更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "更新失败: " + e.getMessage());
        }

        model.addAttribute("admin", admin);
        return "admin/profile";
    }

    @PostMapping("/updatePwd")
    public String updatePwd(@RequestParam String oldPwd,
                            @RequestParam String newPwd,
                            @RequestParam String confirmPwd,
                            HttpSession session,
                            Model model) {
        if (!checkAdminLogin(session)) {
            return "redirect:/admin/web/login";
        }

        User admin = (User) session.getAttribute("loginAdmin");

        try {
            if (!newPwd.equals(confirmPwd)) {
                model.addAttribute("error", "两次输入的新密码不一致！");
                model.addAttribute("admin", admin);
                return "admin/profile";
            }

            if (newPwd == null || newPwd.trim().isEmpty()) {
                model.addAttribute("error", "新密码不能为空！");
                model.addAttribute("admin", admin);
                return "admin/profile";
            }

            String encryptedOldPwd = encryptUtil.saltedPassword(oldPwd);
            if (!admin.getPassword().equals(encryptedOldPwd)) {
                model.addAttribute("error", "原密码错误！");
                model.addAttribute("admin", admin);
                return "admin/profile";
            }

            if (oldPwd.equals(newPwd)) {
                model.addAttribute("error", "新密码不能与原密码相同！");
                model.addAttribute("admin", admin);
                return "admin/profile";
            }

            String encryptedNewPwd = encryptUtil.saltedPassword(newPwd);
            admin.setPassword(encryptedNewPwd);
            User savedAdmin = userRepository.save(admin);

            if (savedAdmin != null) {
                session.setAttribute("loginAdmin", savedAdmin);
                model.addAttribute("success", "密码修改成功！");
                model.addAttribute("admin", savedAdmin);
            } else {
                model.addAttribute("error", "密码修改失败，请重试！");
                model.addAttribute("admin", admin);
            }

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "密码修改失败: " + e.getMessage());
            model.addAttribute("admin", admin);
        }

        return "admin/profile";
    }


    //检验管理员登录状态
    private boolean checkAdminLogin(HttpSession session) {
        User admin = (User) session.getAttribute("loginAdmin");
        return admin != null && "admin".equals(admin.getUserType());
    }

}