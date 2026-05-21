package com.example.jianlou.controller;

import com.example.jianlou.common.EncryptUtil;
import com.example.jianlou.entity.Cookie;
import com.example.jianlou.entity.Post;
import com.example.jianlou.entity.PostImage;
import com.example.jianlou.entity.User;
import com.example.jianlou.repository.CookieRepository;
import com.example.jianlou.repository.PostImageRepository;
import com.example.jianlou.repository.PostRepository;
import com.example.jianlou.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 帖子相关接口控制器
 * 包含：帖子发布、帖子列表查询、单个帖子查询
 */
@RestController
@RequestMapping("/post")
@CrossOrigin // 支持跨域请求，与UserController保持一致
public class PostController {
    // 依赖注入：数据仓库与工具类
    @Resource
    private UserRepository userRepository;
    @Resource
    private CookieRepository cookieRepository;
    @Resource
    private PostRepository postRepository;
    @Resource
    private EncryptUtil encryptUtil;
    @Resource
    private PostImageRepository postImageRepository;

    // 方案1：项目根目录绝对路径
    private static final String PROJECT_ROOT_PATH = System.getProperty("user.dir");
    private static final String UPLOAD_BASE_PATH = PROJECT_ROOT_PATH + "/upload/post/";
    // 方案2：自定义固定绝对路径（注释方案1，启用此方案）
    // private static final String UPLOAD_BASE_PATH = "D:/project_upload/post/";
    private static final String ROOT_URL = "http://192.168.34.31:8080";

    @PostMapping(value = "/publish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
// 移除@RequestParam String postId参数，后端自行生成
    public Map<String, Object> publishPost(
            @RequestParam(value = "cookie", required = true) String cookie,
            @RequestParam String postTitle,
            @RequestParam String postContent,
            @RequestParam String postType,
            @RequestParam String postCommunity,
            @RequestParam String postTime, // 可保留，也可后端自行生成时间
            @RequestParam(value = "postImage", required = false) MultipartFile[] files) {

        // 定义返回结果Map（统一返回JSON格式）
        Map<String, Object> resultMap = new HashMap<>();
        System.out.println("===== PostController接收前端参数 =====");
        System.out.println("前端传递的Cookie原始值：[" + cookie + "]");
        System.out.println("Cookie长度：" + cookie.length());

        // Cookie查询
        String realCookie = cookie.trim();
        Cookie cookieEntity = cookieRepository.findByCookie(realCookie);
        System.out.println("trim后查询到的Cookie对象: " + (cookieEntity == null ? "null" : cookieEntity.toString()));
        if (cookieEntity == null) {
            System.out.println("错误：cookie无效/未登录");
            resultMap.put("code", "failed");
            resultMap.put("msg", "cookie无效");
            return resultMap; // 返回JSON格式错误信息
        }

        // 参数校验（移除postId校验）
        if (postTitle == null || postTitle.trim().isEmpty()
                || postContent == null || postContent.trim().isEmpty()
                || postType == null || postType.trim().isEmpty()
                || postCommunity == null || postCommunity.trim().isEmpty()) {
            System.out.println("参数校验失败：存在空参数");
            resultMap.put("code", "failed");
            resultMap.put("msg", "参数不能为空");
            return resultMap;
        }

        try {
            // 用户验证
            User user = userRepository.findByAccount(cookieEntity.getAccount());
            if (user == null) {
                System.out.println("用户验证失败：用户不存在");
                resultMap.put("code", "failed");
                resultMap.put("msg", "用户不存在");
                return resultMap;
            }

            // 后端自行生成唯一postId（UUID）
            String serverPostId = UUID.randomUUID().toString().replace("-", "");
            System.out.println("后端生成的postId：" + serverPostId);

            // 保存帖子基础信息
            Post post = new Post();
            post.setPostId(serverPostId); // 使用后端生成的postId
            post.setOwner(user);
            post.setPostTitle(postTitle);
            post.setPostContent(postContent);
            post.setPostType(postType);
            post.setPostCommunity(postCommunity);
            post.setPostTime(new Date()); // 优先使用后端时间，更准确
            post.setPostStatus("normal");
            postRepository.save(post);
            System.out.println("帖子基础信息保存成功：postId = " + serverPostId);

            // 处理图片上传（优化路径拼接，解决跨系统兼容问题）
            if (files != null && files.length > 0) {
                // 使用File.separator自动适配Windows(\)和Linux(/)
                String postUploadPath = UPLOAD_BASE_PATH + serverPostId + File.separator;
                File dir = new File(postUploadPath);
                if (!dir.exists()) {
                    boolean mkdirsResult = dir.mkdirs();
                    System.out.println("创建帖子上传目录：" + postUploadPath);
                    System.out.println("创建目录结果：" + (mkdirsResult ? "成功" : "失败"));
                    if (!mkdirsResult) {
                        throw new RuntimeException("创建上传目录失败：" + postUploadPath);
                    }
                }

                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    String originalFileName = file.getOriginalFilename();
                    String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
                    String relativeImagePath = postUploadPath + uniqueFileName;
                    // 图片URL统一使用/拼接，兼容HTTP访问
                    String fullImageUrl = ROOT_URL + "/upload/post/" + serverPostId + "/" + uniqueFileName;
                    File destFile = new File(relativeImagePath);

                    try {
                        file.transferTo(destFile);
                        System.out.println("图片上传成功：" + destFile.getAbsolutePath());
                    } catch (IOException e) {
                        System.out.println("图片写入失败：" + destFile.getAbsolutePath());
                        throw new RuntimeException("图片写入失败：" + e.getMessage());
                    }

                    PostImage postImage = new PostImage();
                    postImage.setPost(post);
                    postImage.setImagePath(relativeImagePath);
                    postImage.setImageUrl(fullImageUrl);
                    postImage.setOriginalName(originalFileName);
                    postImageRepository.save(postImage);
                    System.out.println("图片信息保存成功：" + uniqueFileName);
                }
            }

            System.out.println("帖子发布整体流程完成：postId = " + serverPostId);
            // 返回JSON格式成功信息，包含后端生成的postId
            resultMap.put("code", "success");
            resultMap.put("postId", serverPostId);
            resultMap.put("msg", "发布成功");
            return resultMap;

        } catch (Exception e) {
            System.out.println("帖子发布异常：" + e.getMessage());
            e.printStackTrace();
            resultMap.put("code", "failed");
            resultMap.put("msg", e.getMessage());
            return resultMap;
        }
    }

    /**
     * 帖子列表查询接口
     * 返回所有正常状态的帖子（包含社区信息）
     */
    @PostMapping("/list/")
// 修正2：添加@RequestParam接收前端传递的cookie参数
    public List<Map<String, Object>> getPostList(@RequestParam(value = "cookie", required = false) String cookie) {
        // 可选：添加Cookie合法性校验（与发布接口一致）
        if (cookie == null || cookie.trim().isEmpty()) {
            System.out.println("警告：Cookie为空，返回空列表");
            // 可返回空列表或错误信息，根据业务需求调整
            return new ArrayList<>();
        }
        Cookie cookieEntity = cookieRepository.findByCookie(cookie.trim());
        if (cookieEntity == null) {
            System.out.println("错误：Cookie无效，返回空列表");
            return new ArrayList<>();
        }

        // 修正3：按发布时间倒序排列，新帖子排在最前面
        // 需在PostRepository中添加按状态+时间倒序的查询方法
        List<Post> postList = postRepository.findByPostStatusOrderByPostTimeDesc("normal");
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (Post post : postList) {
            Map<String, Object> postMap = new HashMap<>();
            // 原有字段封装不变...
            postMap.put("postId", post.getPostId());
            postMap.put("postTitle", post.getPostTitle());
            postMap.put("postContent", post.getPostContent());
            postMap.put("postType", post.getPostType());
            postMap.put("postCommunity", post.getPostCommunity());
            postMap.put("postTime", post.getPostTime());
            postMap.put("postStatus", post.getPostStatus());
            postMap.put("userAccount", post.getOwner().getAccount());
            postMap.put("userName", post.getOwner().getUsername());
            // 优化：userHead为空时，返回默认头像URL（避免前端兜底压力）
            String userHead = post.getOwner().getHead() == null || post.getOwner().getHead().trim().isEmpty()
                    ? ROOT_URL + "/default/avatar.png" // 后端配置默认头像路径
                    : ROOT_URL + "/" + post.getOwner().getHead().trim().replace("\\", "/");
            postMap
                    .put("userHead", userHead);

            // 帖子图片列表封装不变...
            List<PostImage> postImageList = postImageRepository.findByPost(post);
            List<Map<String, Object>> imageList = new ArrayList<>();
            for (PostImage postImage : postImageList) {
                Map<String, Object> imageMap = new HashMap<>();
                imageMap.put("imageId", postImage.getId());
                imageMap.put("imagePath", postImage.getImagePath());
                imageMap.put("imageUrl", postImage.getImageUrl());
                imageMap.put("originalName", postImage.getOriginalName());
                imageList.add(imageMap);
            }
            postMap.put("imageList", imageList);

            resultList.add(postMap);
        }

        System.out.println("帖子列表查询完成，共返回 " + resultList.size() + " 条帖子");
        return resultList;
    }

    /**
     * 单个帖子查询接口
     * 根据postId查询帖子详情（包含社区信息）
     */
    @PostMapping("/detail/")
    public Map<String, Object> getPostDetail(@RequestParam String postId) {
        Map<String, Object> resultMap = new HashMap<>();

        // 参数校验
        if (postId == null || postId.trim().isEmpty()) {
            resultMap.put("code", "failed");
            resultMap.put("msg", "帖子ID不能为空");
            return resultMap;
        }

        // 查询帖子
        Post post = postRepository.findByPostId(postId);
        if (post == null) {
            resultMap.put("code", "failed");
            resultMap.put("msg", "帖子不存在或已删除");
            return resultMap;
        }

        // 封装帖子详情、发布者信息...
        resultMap.put("code", "success");
        resultMap.put("postId", post.getPostId());
        resultMap.put("postTitle", post.getPostTitle());
        resultMap.put("postContent", post.getPostContent());
        resultMap.put("postType", post.getPostType());
        resultMap.put("postCommunity", post.getPostCommunity());
        resultMap.put("postTime", post.getPostTime());
        resultMap.put("postStatus", post.getPostStatus());
        resultMap.put("userAccount", post.getOwner().getAccount());
        resultMap.put("userName", post.getOwner().getUsername());
        resultMap.put("userHead", post.getOwner().getHead() == null || post.getOwner().getHead().trim().isEmpty()
                ? "" : ROOT_URL + "/" + post.getOwner().getHead().trim().replace("\\", "/"));

        // 查询帖子图片列表
        List<PostImage> postImageList = postImageRepository.findByPost_PostId(postId);
        List<Map<String, Object>> imageList = new ArrayList<>();
        for (PostImage postImage : postImageList) {
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("imageId", postImage.getId());
            imageMap.put("imagePath", postImage.getImagePath());
            imageMap.put("imageUrl", postImage.getImageUrl());
            imageMap.put("originalName", postImage.getOriginalName());
            imageList.add(imageMap);
        }
        resultMap.put("imageList", imageList); // 返回图片列表

        System.out.println("单个帖子查询完成：postId = " + postId);
        return resultMap;
    }

}

