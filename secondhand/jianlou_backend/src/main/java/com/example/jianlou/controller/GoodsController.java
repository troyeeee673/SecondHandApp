package com.example.jianlou.controller;

import com.example.jianlou.common.EncryptUtil;
import com.example.jianlou.entity.*;
import com.example.jianlou.repository.*;

import com.example.jianlou.service.UserLevelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 商品控制器（与前端PublishGoodActivity完全兼容版）
 * 核心：适配前端Multipart请求、图片上传、参数名对齐
 */
@RestController
@CrossOrigin(origins = "*") // 放宽跨域限制，适配前端本地调试
public class GoodsController {
    @Resource
    private GoodsRepository goodsRepository;
    @Resource
    private GoodsImageRepository goodsImageRepository;
    @Resource
    private UserRepository userRepository;
    @Resource
    private CookieRepository cookieRepository;
    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private OrderRepository orderRepository;


    @Resource
    private CommentRepository commentRepository;

    @Resource  // 添加用户等级服务
    private UserLevelService userLevelService;


    // ======================== 核心：发布商品 ========================
    /**
     * 发布商品接口
     * 前端请求格式：Multipart/form-data
     * 适配前端参数：cookie/content/money/origin_money/send_money/classify/image
     */
    @PostMapping(value = "/publish/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String publish(
            @RequestParam String cookie,
            @RequestParam String content,
            @RequestParam String money,
            @RequestParam("origin_money") String originMoney, // 严格匹配前端下划线参数名
            @RequestParam("send_money") String sendMoney,     // 严格匹配前端下划线参数名
            @RequestParam String classify,
            @RequestParam(value = "image", required = false) MultipartFile[] files) { // 图片非必传（前端已校验）

        // ========== 1. 前端参数前置校验 ==========
        if (cookie == null || cookie.trim().isEmpty()) {
            return "failed";
        }
        if (content == null || content.trim().isEmpty()) {
            return "failed";
        }
        if (money == null || money.trim().isEmpty() || "0".equals(money.trim())) {
            return "failed";
        }
        if (classify == null || classify.trim().isEmpty()) {
            return "failed";
        }

        try {
            // ========== 2. 验证用户合法性 ==========
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "failed"; // cookie无效
            }
            User user = userRepository.findByAccount(cookieEntity.getAccount());
            if (user == null) {
                return "failed"; // 用户不存在
            }

            // ========== 3. 生成商品唯一标识（主键） ==========
            String goodsHash = encryptUtil.md5hex(user.getAccount() + content + System.currentTimeMillis()); // 加时间戳避免重复

            // ========== 4. 保存商品基础信息 ==========
            Goods goods = new Goods();
            goods.setOwner(user);
            goods.setHash(goodsHash);
            goods.setContent(content);
            goods.setMoney(money);
            goods.setOriginMoney(originMoney == null ? "0" : originMoney); // 兼容前端空值
            goods.setSendMoney(sendMoney == null ? "0" : sendMoney);       // 兼容前端空值
            goods.setClassify(classify);
            goods.setEditDate(new Date());
            goodsRepository.save(goods);

            // ========== 5. 图片上传 ==========
            if (files != null && files.length > 0) {
                // 5.1 构建绝对上传路径（兼容Windows/Linux）
                String projectRoot = System.getProperty("user.dir");
                String uploadBasePath = projectRoot + "/upload/";
                String userUploadPath = uploadBasePath + user.getAccount() + "/" + goodsHash + "/";

                // 5.2 强制创建多级目录
                File uploadDir = new File(userUploadPath);
                if (!uploadDir.exists()) {
                    boolean mkdirSuccess = uploadDir.mkdirs();
                    if (!mkdirSuccess) {
                        // 目录创建失败仍返回success（商品已保存，仅图片失败）
                        return "success";
                    }
                }

                // 5.3 遍历处理每张图片（兼容前端多图上传）
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue; // 跳过空文件
                    }

                    // 5.4 生成安全文件名（避免特殊字符/重复）
                    String originalFileName = file.getOriginalFilename();
                    String safeFileName = UUID.randomUUID().toString() + "_" +
                            (originalFileName == null ? "default_img.png" : originalFileName);
                    // 过滤非法字符（Windows/Linux通用）
                    safeFileName = safeFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

                    // 5.5 保存图片到服务器
                    File destFile = new File(userUploadPath + safeFileName);
                    try {
                        file.transferTo(destFile); // 保存图片文件

                        // 5.6 保存图片记录到数据库（存储相对路径，方便前端访问）
                        // 保存图片记录到数据库时，直接存储 /upload/开头的路径（和WebConfig映射一致）
                        GoodsImage goodsImage = new GoodsImage();
                        goodsImage.setGoods(goods);
// 正确路径：/upload/用户ID/商品ID/文件名
                        String relativePath = "/upload/" + user.getAccount() + "/" + goodsHash + "/" + safeFileName;
                        goodsImage.setImage(relativePath);
                        goodsImageRepository.save(goodsImage);
                    } catch (IOException e) {
                        // 单张图片失败不影响整体发布，仅打印日志
                        e.printStackTrace();
                        continue;
                    }
                    // ========== 6. 发布商品成功，增加用户活跃度 ==========
                    try {
                        userLevelService.handleGoodsPublish(user.getAccount());
                    } catch (Exception e) {
                        System.err.println("更新用户活跃度失败，但商品发布成功: " + e.getMessage());
                        // 这里不返回失败，因为商品发布已经成功
                    }
                }
            }

            // ========== 6. 发布成功 ==========
            return "success";
        } catch (Exception e) {
            // 捕获所有异常，避免前端请求崩溃
            e.printStackTrace();
            return "failed";
        }
    }

    // ======================== 编辑商品 ========================
    @PostMapping("/edit/")
    public String edit(
            @RequestParam String cookie,
            @RequestParam String hash,
            @RequestParam String content,
            @RequestParam String price) {
        try {
            // 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "failed";
            }
            // 验证商品
            Goods goods = goodsRepository.findById(hash).orElse(null);
            if (goods == null) {
                return "failed";
            }
            // 验证商品归属
            if (!goods.getOwner().getAccount().equals(cookieEntity.getAccount())) {
                return "failed";
            }

            // 更新商品信息（不修改主键hash）
            goods.setContent(content);
            goods.setMoney(price);
            goods.setEditDate(new Date());
            goodsRepository.save(goods);

            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    // ======================== 首页商品列表（只显示在售且审核已通过的商品） ========================
    @GetMapping("/get_goods")
    public List<Map<String, Object>> getGoods() {
        // 只查询状态为 on_sale 且审核状态为 approved 的商品
        List<Goods> allGoods = goodsRepository.findByStatusOrderByEditDateDesc("on_sale");
        List<Goods> goodsList = new ArrayList<>();

        // 过滤出已审核通过的商品
        for (Goods goods : allGoods) {
            // 确保审核状态是 approved
            if ("approved".equals(goods.getAuditStatus())) {
                goodsList.add(goods);
            }
        }

        //格式化商品数据（适配前端）
        List<Map<String, Object>> result = new ArrayList<>();

        for (Goods goods : goodsList) {
            Map<String, Object> goodsMap = new HashMap<>();
            // 基础信息
            goodsMap.put("goodsID", goods.getHash());
            goodsMap.put("content", goods.getContent());
            goodsMap.put("money", goods.getMoney());
            goodsMap.put("user_name", goods.getOwner().getUsername());
            goodsMap.put("status", goods.getStatus()); // 返回状态信息
            goodsMap.put("auditStatus", goods.getAuditStatus()); // 也可以返回审核状态（可选）

            // 商品首图
            List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
            if (images != null && !images.isEmpty()) {
                String imagePath = images.get(0).getImage();
                if (imagePath.startsWith("/upload/")) {
                    goodsMap.put("image", "http://192.168.34.31:8080" + imagePath);
                } else if (imagePath.startsWith("http")) {
                    goodsMap.put("image", imagePath);
                } else {
                    goodsMap.put("image", "http://192.168.34.31:8080/upload/" + imagePath);
                }
            } else {
                goodsMap.put("image", "");
            }

            result.add(goodsMap);
        }
        return result;
    }

    // 兼容前端原有/index/接口（POST请求）
    @PostMapping("/index/")
    public List<Map<String, Object>> getGoodsByIndex() {
        return getGoods();
    }

    // ======================== 商品详情（与前端/goods/detail/兼容） ========================
    @PostMapping("/goods/detail/")
    public Object goodsDetail(@RequestParam String goodsID) {
        try {
            Goods goods = goodsRepository.findById(goodsID).orElse(null);
            if (goods == null) {
                return "failed";
            }

            Map<String, Object> detail = new HashMap<>();
            // 用户信息
            detail.put("username", goods.getOwner().getAccount());
            detail.put("user_name", goods.getOwner().getUsername());
            detail.put("seller_account", goods.getOwner().getAccount()); // 添加卖家账号

            // ========== 新增：卖家用户ID和点赞数 ==========
            detail.put("seller_user_id", goods.getOwner().getAccount()); // 卖家用户ID
            detail.put("like_count", goods.getLikesCount()); // 商品点赞数
            // ===========================================

            // 商品基础信息
            detail.put("money", goods.getMoney());
            detail.put("content", goods.getContent());
            detail.put("origin_money", goods.getOriginMoney());
            detail.put("send_money", goods.getSendMoney());

            // 商品图片列表
            List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
            List<Map<String, String>> imageList = new ArrayList<>();
            for (GoodsImage img : images) {
                Map<String, String> imgMap = new HashMap<>();
                imgMap.put("image", img.getImage());
                imageList.add(imgMap);
            }
            detail.put("images", imageList);

            // 时间格式化
            detail.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(goods.getEditDate()));

            return detail;
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    // ======================== 我的商品（与前端/mygoods/兼容） ========================
    @PostMapping("/mygoods/")
    public Object myGoods(@RequestParam String cookie) {
        try {
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "failed";
            }
            User user = userRepository.findByAccount(cookieEntity.getAccount());
            // 修改：返回所有状态的商品，包括已售出的
            List<Goods> goodsList = goodsRepository.findByOwnerOrderByEditDateDesc(user); // 返回所有商品

            List<Map<String, Object>> myGoods = new ArrayList<>();
            for (Goods goods : goodsList) {
                Map<String, Object> goodsMap = new HashMap<>();
                goodsMap.put("goodsID", goods.getHash());
                goodsMap.put("content", goods.getContent());
                goodsMap.put("money", goods.getMoney());
                goodsMap.put("status", goods.getStatus()); // 商品状态：on_sale, sold, off_shelf

                // 商品首图 - 返回完整URL
                List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
                if (images != null && !images.isEmpty()) {
                    String imagePath = images.get(0).getImage();
                    if (imagePath.startsWith("/upload/")) {
                        goodsMap.put("image", "http://192.168.34.31:8080" + imagePath);
                    } else if (imagePath.startsWith("http")) {
                        goodsMap.put("image", imagePath);
                    } else {
                        goodsMap.put("image", "http://192.168.34.31:8080/upload/" + imagePath);
                    }
                } else {
                    goodsMap.put("image", "");
                }

                myGoods.add(goodsMap);
            }
            return myGoods;
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    // ======================== 辅助：静态资源映射（图片访问） ========================
    // 需配合application.yml中的spring.web.resources.static-locations配置
    // 配置示例：
    // spring:
    //   web:
    //     resources:
    //       static-locations: file:${user.dir}/upload/,classpath:/static/
    // ======================== 删除商品 ========================
    @PostMapping("/delete_goods/")
    public String deleteGoods(
            @RequestParam String cookie,
            @RequestParam String goodsID) {
        try {
            // 验证cookie
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "cookie_invalid"; // 返回具体错误信息
            }

            // 验证商品存在
            Goods goods = goodsRepository.findById(goodsID).orElse(null);
            if (goods == null) {
                return "goods_not_found";
            }

            // 验证商品归属（只有发布者才能删除）
            if (!goods.getOwner().getAccount().equals(cookieEntity.getAccount())) {
                return "no_permission";
            }

            // 删除商品图片
            List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
            if (images != null && !images.isEmpty()) {
                // 删除图片文件（可选）
                for (GoodsImage image : images) {
                    String imagePath = System.getProperty("user.dir") + image.getImage();
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
                // 删除数据库中的图片记录
                goodsImageRepository.deleteAll(images);
            }

            // 删除商品
            goodsRepository.delete(goods);

            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    // ======================== LY商品搜索接口========================
    @PostMapping("/search/")
    public Object searchGoods(
            @RequestParam String q,          // 搜索关键词
            @RequestParam(required = false, defaultValue = "1") String method, // 排序方式id
            @RequestParam(required = false, defaultValue = "1") String page,   // 页码
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        try {
            List<Goods> goodsList;
            int pageNum = Integer.parseInt(page);
            int pageSize = 10;

            double min = (minPrice != null) ? minPrice : 0;
            double max = (maxPrice != null) ? maxPrice : Double.MAX_VALUE;

            // 根据前端传来的排序方式id进行分支处理
            switch (method) {
                case "4": // 价格升序 (ID=4)
                    if (minPrice != null || maxPrice != null) {
                        goodsList = goodsRepository.findByContentAndPriceRangeOrderByPriceAsc(q, "on_sale", min, max);
                    } else {
                        goodsList = goodsRepository.findByContentContainingAndStatusOrderByMoneyAsc(q, "on_sale");
                    }
                    break;

                case "5": // 价格降序 (ID=5)
                    if (minPrice != null || maxPrice != null) {
                        goodsList = goodsRepository.findByContentAndPriceRangeOrderByPriceDesc(q, "on_sale", min, max);
                    } else {
                        goodsList = goodsRepository.findByContentContainingAndStatusOrderByMoneyDesc(q, "on_sale");
                    }
                    break;

                case "2": // 时间正序 (ID=2)
                    goodsList = goodsRepository.findByContentContainingAndStatusOrderByEditDateDesc(q, "on_sale");
                    Collections.reverse(goodsList); // 翻转为最旧的在前面
                    if (minPrice != null || maxPrice != null) goodsList = filterByPrice(goodsList, min, max);
                    break;

                case "1": // 综合排序
                case "3": // 时间倒序 (ID=3)
                default:
                    goodsList = goodsRepository.findByContentContainingAndStatusOrderByEditDateDesc(q, "on_sale");
                    if (minPrice != null || maxPrice != null) goodsList = filterByPrice(goodsList, min, max);
                    break;
            }

            // 分页与结果封装
            int total = goodsList.size();
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, total);
            int totalPage = (int) Math.ceil((double) total / pageSize);

            List<Map<String, Object>> result = new ArrayList<>();
            if (start < total) {
                List<Goods> pageData = goodsList.subList(start, end);
                for (Goods goods : pageData) {
                    Map<String, Object> goodsMap = new HashMap<>();
                    goodsMap.put("goodsID", goods.getHash());
                    goodsMap.put("content", goods.getContent());
                    goodsMap.put("money", goods.getMoney());
                    goodsMap.put("user_name", goods.getOwner().getUsername());

                    List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
                    String imgUrl = (images != null && !images.isEmpty()) ? images.get(0).getImage() : "";
                    if (!imgUrl.isEmpty()) {
                        goodsMap.put("image", imgUrl.startsWith("/upload/") ? "http://192.168.34.31:8080" + imgUrl : "http://192.168.34.31:8080/upload/" + imgUrl);
                    } else {
                        goodsMap.put("image", "");
                    }
                    result.add(goodsMap);
                }
            }

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("max_page", totalPage);
            pageInfo.put("current_page", pageNum);
            pageInfo.put("total_items", total);
            result.add(pageInfo);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    // 手动过滤价格（用于时间排序+价格筛选）
    private List<Goods> filterByPrice(List<Goods> goodsList, double minPrice, double maxPrice) {
        List<Goods> filteredList = new ArrayList<>();

        for (Goods goods : goodsList) {
            try {
                double price = Double.parseDouble(goods.getMoney());
                if (price >= minPrice && price <= maxPrice) {
                    filteredList.add(goods);
                }
            } catch (NumberFormatException e) {
                // 如果价格格式错误，跳过这个商品
                continue;
            }
        }

        return filteredList;
    }


    // ======================== 按分类搜索商品 ========================
    @PostMapping("/search/byClassify/")
    public Object searchByClassify(
            @RequestParam String classify,
            @RequestParam(required = false, defaultValue = "1") String page,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "1") String sortType) { // 对应前端的id

        try {
            List<Goods> goodsList;
            double min = (minPrice != null) ? minPrice : 0;
            double max = (maxPrice != null) ? maxPrice : Double.MAX_VALUE;

            // 根据 sortType 选择特定的查询方法
            if ("4".equals(sortType)) { // 价格升序
                if (minPrice != null || maxPrice != null) {
                    goodsList = goodsRepository.findByClassifyAndPriceRangeOrderByPriceAsc(classify, "on_sale", min, max);
                } else {
                    goodsList = goodsRepository.findByClassifyContainingAndStatusOrderByMoneyAsc(classify, "on_sale");
                }
            } else if ("5".equals(sortType)) { // 价格降序
                if (minPrice != null || maxPrice != null) {
                    goodsList = goodsRepository.findByClassifyAndPriceRangeOrderByPriceDesc(classify, "on_sale", min, max);
                } else {
                    goodsList = goodsRepository.findByClassifyContainingAndStatusOrderByMoneyDesc(classify, "on_sale");
                }
            } else if ("2".equals(sortType)) { // 时间正序
                goodsList = goodsRepository.findByClassifyContainingAndStatusOrderByEditDateDesc(classify, "on_sale");
                Collections.reverse(goodsList);
                if (minPrice != null || maxPrice != null) goodsList = filterByPrice(goodsList, min, max);
            } else { // 综合(1) 或 时间倒序(3)
                goodsList = goodsRepository.findByClassifyContainingAndStatusOrderByEditDateDesc(classify, "on_sale");
                if (minPrice != null || maxPrice != null) goodsList = filterByPrice(goodsList, min, max);
            }

            // 分页逻辑
            int pageNum = Integer.parseInt(page);
            int pageSize = 10;
            int total = goodsList.size();
            int totalPage = (int) Math.ceil((double) total / pageSize);
            int start = (pageNum - 1) * pageSize;

            List<Map<String, Object>> result = new ArrayList<>();
            if (start < total) {
                List<Goods> pageData = goodsList.subList(start, Math.min(start + pageSize, total));
                for (Goods goods : pageData) {
                    Map<String, Object> goodsMap = new HashMap<>();
                    goodsMap.put("goodsID", goods.getHash());
                    goodsMap.put("content", goods.getContent());
                    goodsMap.put("money", goods.getMoney());
                    goodsMap.put("user_name", goods.getOwner().getUsername());

                    List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
                    String imgUrl = (images != null && !images.isEmpty()) ? images.get(0).getImage() : "";
                    if (!imgUrl.isEmpty()) {
                        goodsMap.put("image", "http://192.168.34.31:8080" + (imgUrl.startsWith("/upload/") ? imgUrl : "/upload/" + imgUrl));
                    } else {
                        goodsMap.put("image", "");
                    }
                    result.add(goodsMap);
                }
            }

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("max_page", totalPage);
            pageInfo.put("current_page", pageNum);
            pageInfo.put("total_items", total);
            result.add(pageInfo);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

// ======================== 添加评论接口 ========================
    @PostMapping("/comments/add/")
    public String addComment(
            @RequestParam String cookie,
            @RequestParam String goodsID,
            @RequestParam String content) {

        try {
            // 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "cookie_invalid";
            }

            User user = userRepository.findByAccount(cookieEntity.getAccount());
            if (user == null) {
                return "user_not_found";
            }

            // 验证商品存在
            Goods goods = goodsRepository.findById(goodsID).orElse(null);
            if (goods == null) {
                return "goods_not_found";
            }

            // 创建评论实体
            Comment comment = new Comment();
            comment.setCommentId(encryptUtil.md5hex(user.getAccount() + goodsID + System.currentTimeMillis()));
            comment.setGoods(goods);
            comment.setUser(user);
            comment.setContent(content);
            comment.setCreateTime(new Date());

            // 保存评论
            commentRepository.save(comment);

            // 调用用户等级服务处理评论
            String reviewerAccount = user.getAccount();
            userLevelService.handleReviewSubmit(reviewerAccount);

            System.out.println("评论提交，触发用户等级计算: 评论者=" + reviewerAccount);

            return "success";

        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    // ======================== 获取商品评论接口 ========================
    @PostMapping("/comments/get/")
    public Object getComments(@RequestParam String goodsID) {

        try {
            // 验证商品存在
            Goods goods = goodsRepository.findById(goodsID).orElse(null);
            if (goods == null) {
                return "failed";
            }

            // 获取该商品的所有评论，按时间倒序（修改这里）
            List<Comment> comments = commentRepository.findByGoodsOrderByCreateTimeDesc(goods);

            List<Map<String, Object>> result = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (Comment comment : comments) {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("commentId", comment.getCommentId());
                commentMap.put("goodsId", goodsID);
                commentMap.put("userId", comment.getUser().getAccount());
                commentMap.put("userName", comment.getUser().getUsername());
                commentMap.put("content", comment.getContent());
                commentMap.put("createTime", sdf.format(comment.getCreateTime()));

                result.add(commentMap);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    /**
     * 立即购买接口
     * @param cookie 买家的cookie（用于验证身份）
     * @param goodsID 商品ID
     * @return 操作结果
     */
    @PostMapping("/buy/now/")
    public String buyNow(
            @RequestParam String cookie,
            @RequestParam String goodsID) {
        try {
            // 1. 验证用户（买家）
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "failed";
            }
            User buyer = userRepository.findByAccount(cookieEntity.getAccount());
            if (buyer == null) {
                return "failed";
            }

            // 2. 验证商品
            Goods goods = goodsRepository.findById(goodsID).orElse(null);
            if (goods == null) {
                return "failed";
            }

            // 3. 获取卖家
            User seller = goods.getOwner();

            // 4. 生成订单ID
            String orderId = encryptUtil.md5hex(buyer.getAccount() + goodsID + System.currentTimeMillis());

            // 5. 创建并保存订单
            Order order = new Order();
            order.setOrderId(orderId);
            order.setGoods(goods);
            order.setBuyer(buyer);
            order.setSeller(seller);
            order.setCreateTime(new Date());
            orderRepository.save(order);

            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }


    /**
     * 创建订单接口
     * 请求参数：cookie, goods_id, seller_account
     */
    // ======================== 创建订单接口（修改版） ========================
    @PostMapping("/orders/create/")
    public String createOrder(
            @RequestParam String cookie,
            @RequestParam String goods_id,
            @RequestParam String seller_account) {

        try {
            System.out.println("=== 创建订单并更新商品状态 ===");

            // 1. 验证用户（买家）
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "{\"status\":\"error\",\"message\":\"用户未登录\"}";
            }
            String buyerAccount = cookieEntity.getAccount();
            User buyer = userRepository.findByAccount(buyerAccount);
            if (buyer == null) {
                return "{\"status\":\"error\",\"message\":\"用户不存在\"}";
            }

            // 2. 验证商品
            Goods goods = goodsRepository.findById(goods_id).orElse(null);
            if (goods == null) {
                return "{\"status\":\"error\",\"message\":\"商品不存在\"}";
            }

            // 3. 检查商品状态
            if (Goods.STATUS_SOLD.equals(goods.getStatus())) {
                return "{\"status\":\"error\",\"message\":\"商品已售出\"}";
            }
            if (Goods.STATUS_OFF_SHELF.equals(goods.getStatus())) {
                return "{\"status\":\"error\",\"message\":\"商品已下架\"}";
            }

            // 4. 验证卖家
            User seller = userRepository.findByAccount(seller_account);
            if (seller == null) {
                return "{\"status\":\"error\",\"message\":\"卖家不存在\"}";
            }

            // 5. 检查是否购买自己的商品
            if (buyer.getAccount().equals(seller.getAccount())) {
                return "{\"status\":\"error\",\"message\":\"不能购买自己的商品\"}";
            }

            // 6. 生成订单ID
            String orderId = "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);

            // 7. 创建并保存订单
            Order order = new Order();
            order.setOrderId(orderId);
            order.setGoods(goods);
            order.setBuyer(buyer);
            order.setSeller(seller);
            order.setCreateTime(new Date());
            order.setStatus(Order.STATUS_PENDING); // 设置初始状态

            orderRepository.save(order);

            // 8. 更新商品状态为已售出
            goods.setStatus(Goods.STATUS_SOLD);
            goods.setEditDate(new Date());
            goodsRepository.save(goods);

            System.out.println("订单创建成功，ID: " + orderId);

            return "{\"status\":\"success\",\"order_id\":\"" + orderId + "\"}";

        } catch (Exception e) {
            System.err.println("创建订单异常: " + e.getMessage());
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"创建失败: " + e.getMessage() + "\"}";
        }
    }


    // ======================== 获取我买到的订单 ========================
    @PostMapping("/orders/my_bought/")
    public String getMyBoughtOrders(@RequestParam String cookie) {
        try {
            System.out.println("=== 查询我买到的订单 ===");

            // 1. 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "{\"status\":\"error\",\"message\":\"用户未登录\"}";
            }

            // 2. 获取用户账号
            String userAccount = cookieEntity.getAccount();
            System.out.println("用户账号: " + userAccount);

            // 3. 查询订单 - 使用自定义查询方法
            List<Order> orders = orderRepository.findByBuyerAccount(userAccount);

            System.out.println("查询到订单数量: " + (orders != null ? orders.size() : 0));

            // 构建返回数据
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode jsonArray = mapper.createArrayNode();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            if (orders != null && !orders.isEmpty()) {
                System.out.println("开始处理 " + orders.size() + " 个订单...");

                for (Order order : orders) {
                    try {
                        ObjectNode orderObj = mapper.createObjectNode();

                        // 1. 订单基本信息
                        orderObj.put("order_id", order.getOrderId() != null ? order.getOrderId() : "");
                        orderObj.put("create_time", order.getCreateTime() != null ?
                                sdf.format(order.getCreateTime()) : "");

                        // ********** 核心修改：添加订单状态字段 **********
                        // 使用实体类中定义的常量或默认值
                        String orderStatus = order.getStatus() != null ? order.getStatus() : Order.STATUS_PENDING;
                        orderObj.put("status", orderStatus);
                        System.out.println("订单 " + order.getOrderId() + " 状态: " + orderStatus);

                        // 2. 商品信息
                        Goods goods = order.getGoods();
                        if (goods != null) {
                            // 商品ID
                            orderObj.put("goods_id", goods.getHash() != null ? goods.getHash() : "");

                            // 商品名称（截取处理）
                            String goodsName = goods.getContent() != null ? goods.getContent() : "未命名商品";
                            if (goodsName.length() > 20) {
                                goodsName = goodsName.substring(0, 20) + "...";
                            }
                            orderObj.put("goods_name", goodsName);

                            // 商品价格
                            String price = goods.getMoney() != null ? goods.getMoney() : "0.00";
                            orderObj.put("price", price);

                            // 商品图片 - 获取首张图片
                            String imageUrl = "";
                            List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
                            if (images != null && !images.isEmpty()) {
                                GoodsImage firstImage = images.get(0);
                                if (firstImage != null && firstImage.getImage() != null) {
                                    String imagePath = firstImage.getImage();
                                    if (imagePath.startsWith("/upload/")) {
                                        imageUrl = "http://192.168.34.31:8080" + imagePath;
                                    } else if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                                        imageUrl = imagePath;
                                    } else {
                                        imageUrl = "http://192.168.34.31:8080/upload/" + imagePath;
                                    }
                                }
                            }
                            orderObj.put("goods_image", imageUrl.isEmpty() ? "" : imageUrl);

                            // 添加商品状态（如果需要）
                            orderObj.put("goods_status", goods.getStatus() != null ? goods.getStatus() : "");

                        } else {
                            // 商品为null的情况（可能已被删除）
                            orderObj.put("goods_id", "");
                            orderObj.put("goods_name", "商品已删除");
                            orderObj.put("price", "0.00");
                            orderObj.put("goods_image", "");
                            orderObj.put("goods_status", "");
                        }

                        // 3. 卖家信息
                        User seller = order.getSeller();
                        if (seller != null) {
                            orderObj.put("seller_name", seller.getUsername() != null ?
                                    seller.getUsername() : "未知卖家");
                            orderObj.put("seller_account", seller.getAccount() != null ?
                                    seller.getAccount() : "");
                        } else {
                            orderObj.put("seller_name", "未知卖家");
                            orderObj.put("seller_account", "");
                        }

                        // 4. 买家信息（可选，如果需要）
                        User buyer = order.getBuyer();
                        if (buyer != null) {
                            orderObj.put("buyer_name", buyer.getUsername() != null ?
                                    buyer.getUsername() : "");
                            orderObj.put("buyer_account", buyer.getAccount() != null ?
                                    buyer.getAccount() : "");
                        }

                        // 5. 完成时间（如果订单已完成）
                        if (order.getCompleteTime() != null) {
                            orderObj.put("complete_time", sdf.format(order.getCompleteTime()));
                        }

                        jsonArray.add(orderObj);

                    } catch (Exception e) {
                        System.err.println("处理订单时发生错误，订单ID: " +
                                (order != null ? order.getOrderId() : "null") +
                                ", 错误信息: " + e.getMessage());
                        e.printStackTrace();
                        continue;
                    }
                }
            } else {
                System.out.println("没有找到任何订单");
            }

            // 构建返回结果
            ObjectNode result = mapper.createObjectNode();
            result.put("status", "success");
            result.set("orders", jsonArray);
            result.put("total", jsonArray.size());
            result.put("message", jsonArray.size() > 0 ? "查询成功，共" + jsonArray.size() + "个订单" : "暂无购买记录");

            // 打印调试信息
            System.out.println("=== 返回的JSON数据 ===");
            String resultJson = result.toString();
            System.out.println(resultJson.substring(0, Math.min(resultJson.length(), 500)) + "...");
            System.out.println("====================");

            return resultJson;

        } catch (Exception e) {
            System.err.println("获取我买到的订单异常: " + e.getMessage());
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"查询失败: " + e.getMessage() + "\"}";
        }
    }

    // ======================== 获取我卖出的订单 ========================
    @PostMapping("/orders/my_sold/")
    public String getMySoldOrders(@RequestParam String cookie) {
        try {
            System.out.println("=== 查询我卖出的订单 ===");

            // 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "{\"status\":\"error\",\"message\":\"用户未登录\"}";
            }
            User user = userRepository.findByAccount(cookieEntity.getAccount());
            if (user == null) {
                return "{\"status\":\"error\",\"message\":\"用户不存在\"}";
            }

            System.out.println("查询卖家: " + user.getAccount());

            // 获取该用户卖出的所有订单
            List<Order> orders = orderRepository.findBySeller(user);
            System.out.println("查询到订单数量: " + orders.size());

            // 使用 Jackson
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode jsonArray = mapper.createArrayNode();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (Order order : orders) {
                System.out.println("处理订单: " + order.getOrderId() + ", 状态: " + order.getStatus());

                ObjectNode orderObj = mapper.createObjectNode();

                // 订单基本信息
                orderObj.put("order_id", order.getOrderId());
                orderObj.put("create_time", sdf.format(order.getCreateTime()));

                // ********** 关键：添加订单状态字段 **********
                String orderStatus = order.getStatus() != null ? order.getStatus() : Order.STATUS_PENDING;
                orderObj.put("status", orderStatus);
                System.out.println("订单 " + order.getOrderId() + " 状态: " + orderStatus);

                // 商品信息
                Goods goods = order.getGoods();
                if (goods != null) {
                    orderObj.put("goods_id", goods.getHash());
                    orderObj.put("goods_name", goods.getContent().length() > 20 ?
                            goods.getContent().substring(0, 20) + "..." : goods.getContent());
                    orderObj.put("price", goods.getMoney());

                    // 获取商品首图
                    List<GoodsImage> images = goodsImageRepository.findByGoods(goods);
                    if (images != null && !images.isEmpty()) {
                        String imagePath = images.get(0).getImage();
                        if (imagePath.startsWith("/upload/")) {
                            orderObj.put("goods_image", "http://192.168.34.31:8080" + imagePath);
                        } else {
                            orderObj.put("goods_image", "http://192.168.34.31:8080/upload/" + imagePath);
                        }
                    } else {
                        orderObj.put("goods_image", "");
                    }
                }

                // 买家信息
                User buyer = order.getBuyer();
                if (buyer != null) {
                    orderObj.put("buyer_name", buyer.getUsername());
                    orderObj.put("buyer_account", buyer.getAccount());
                }

                jsonArray.add(orderObj);
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("status", "success");
            result.set("orders", jsonArray);
            result.put("total", jsonArray.size());

            String resultJson = result.toString();
            System.out.println("返回JSON数据: " + resultJson.substring(0, Math.min(resultJson.length(), 500)) + "...");

            return resultJson;

        } catch (Exception e) {
            System.err.println("获取我卖出的订单异常: " + e.getMessage());
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"服务器错误: " + e.getMessage() + "\"}";
        }
    }

    // ======================== 新增：确认收货/完成订单 ========================
    @PostMapping("/orders/complete/")
    public String completeOrder(
            @RequestParam String cookie,
            @RequestParam String order_id) {
        try {
            // 1. 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "{\"status\":\"error\",\"message\":\"用户未登录\"}";
            }
            User user = userRepository.findByAccount(cookieEntity.getAccount());
            if (user == null) {
                return "{\"status\":\"error\",\"message\":\"用户不存在\"}";
            }

            // 2. 查询订单
            Order order = orderRepository.findById(order_id).orElse(null);
            if (order == null) {
                return "{\"status\":\"error\",\"message\":\"订单不存在\"}";
            }

            // 3. 验证权限（只有买家可以确认收货）
            if (!order.getBuyer().getAccount().equals(user.getAccount())) {
                return "{\"status\":\"error\",\"message\":\"只有买家可以确认收货\"}";
            }


            // 4. 更新订单状态为已完成
            order.setStatus(Order.STATUS_COMPLETED);
            order.setCompleteTime(new Date());
            orderRepository.save(order);

            // 5. 调用用户等级服务处理交易完成
            String buyerAccount = order.getBuyer().getAccount();
            String sellerAccount = order.getSeller().getAccount();

            userLevelService.handleTransactionComplete(buyerAccount, sellerAccount);

            System.out.println("订单完成，触发等级计算: 买家=" + buyerAccount + ", 卖家=" + sellerAccount);

            // 不要在这里调用 userRepository.save() 或其他保存操作
            return "{\"status\":\"success\",\"message\":\"确认收货成功\"}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"确认收货失败\"}";
        }
    }


    // ======================== 新增：退货处理 ========================
    @PostMapping("/orders/return/")
    public String returnOrder(
            @RequestParam String cookie,
            @RequestParam String order_id,
            @RequestParam String reason) {
        try {
            // 1. 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                return "{\"status\":\"error\",\"message\":\"用户未登录\"}";
            }
            User user = userRepository.findByAccount(cookieEntity.getAccount());
            if (user == null) {
                return "{\"status\":\"error\",\"message\":\"用户不存在\"}";
            }

            // 2. 查询订单
            Order order = orderRepository.findById(order_id).orElse(null);
            if (order == null) {
                return "{\"status\":\"error\",\"message\":\"订单不存在\"}";
            }

            // 3. 验证权限（只有买家可以申请退货）
            if (!order.getBuyer().getAccount().equals(user.getAccount())) {
                return "{\"status\":\"error\",\"message\":\"只有买家可以申请退货\"}";
            }

            // 4. 更新订单状态为已退货
            order.setStatus(Order.STATUS_RETURNED);
            orderRepository.save(order);

            // 5. 恢复商品状态为上架（如果需要）
            Goods goods = order.getGoods();
            if (goods != null) {
                goods.setStatus(Goods.STATUS_ON_SALE);
                goodsRepository.save(goods);
            }

            // 6. 调用用户等级服务处理退货
            String sellerAccount = order.getSeller().getAccount();
            userLevelService.handleReturn(sellerAccount);

            System.out.println("退货处理，触发卖家信誉度扣分: 卖家=" + sellerAccount);

            return "{\"status\":\"success\",\"message\":\"退货申请已提交\"}";

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"退货申请失败\"}";
        }
    }

}