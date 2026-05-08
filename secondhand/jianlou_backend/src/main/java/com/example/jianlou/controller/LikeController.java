package com.example.jianlou.controller;

import com.example.jianlou.entity.*;
import com.example.jianlou.repository.*;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/like")
@CrossOrigin
public class LikeController {
    @Resource
    private LikeRecordRepository likeRecordRepository;

    @Resource
    private CookieRepository cookieRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private GoodsRepository goodsRepository;

    /**
     * 点赞/取消点赞接口
     */
    @PostMapping("/toggle/")
    public Map<String, Object> toggleLike(
            @RequestParam String cookie,
            @RequestParam String goodsID) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            String userAccount = cookieEntity.getAccount();
            User user = userRepository.findByAccount(userAccount);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 验证商品
            Goods goods = goodsRepository.findById(goodsID).orElse(null);
            if (goods == null) {
                result.put("success", false);
                result.put("message", "商品不存在");
                return result;
            }

            // 检查是否是自己发布的商品
            if (goods.getOwner().getAccount().equals(userAccount)) {
                result.put("success", false);
                result.put("message", "不能给自己的商品点赞");
                return result;
            }

            // 检查是否已经点赞
            boolean isLiked = likeRecordRepository.existsByUserAccountAndGoodsHash(userAccount, goodsID);

            if (isLiked) {
                // 取消点赞
                likeRecordRepository.findByUserAccountAndGoodsHash(userAccount, goodsID)
                        .ifPresent(likeRecordRepository::delete);

                // 更新商品点赞数
                goods.setLikesCount(Math.max(0, goods.getLikesCount() - 1));
                goodsRepository.save(goods);

                // 更新用户收到的赞数
                User goodsOwner = goods.getOwner();
                goodsOwner.setLikesReceived(Math.max(0, goodsOwner.getLikesReceived() - 1));
                userRepository.save(goodsOwner);

                result.put("success", true);
                result.put("message", "已取消点赞");
                result.put("liked", false);
            } else {
                // 点赞
                LikeRecord likeRecord = new LikeRecord();
                likeRecord.setUser(user);
                likeRecord.setGoods(goods);
                likeRecord.setUniqueKey(userAccount + "_" + goodsID);
                likeRecordRepository.save(likeRecord);

                // 更新商品点赞数
                goods.setLikesCount(goods.getLikesCount() + 1);
                goodsRepository.save(goods);

                // 更新用户收到的赞数
                User goodsOwner = goods.getOwner();
                goodsOwner.setLikesReceived(goodsOwner.getLikesReceived() + 1);
                userRepository.save(goodsOwner);

                result.put("success", true);
                result.put("message", "点赞成功");
                result.put("liked", true);
            }

            // 返回最新的点赞数
            result.put("like_count", goods.getLikesCount());

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取点赞状态接口
     */
    @PostMapping("/status/")
    public Map<String, Object> getLikeStatus(
            @RequestParam String cookie,
            @RequestParam String goodsID) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 验证用户
            Cookie cookieEntity = cookieRepository.findByCookie(cookie);
            if (cookieEntity == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            String userAccount = cookieEntity.getAccount();

            // 验证商品
            Goods goods = goodsRepository.findById(goodsID).orElse(null);
            if (goods == null) {
                result.put("success", false);
                result.put("message", "商品不存在");
                return result;
            }

            // 检查是否已经点赞
            boolean isLiked = likeRecordRepository.existsByUserAccountAndGoodsHash(userAccount, goodsID);

            result.put("success", true);
            result.put("liked", isLiked);
            result.put("like_count", goods.getLikesCount());

        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "查询失败");
        }

        return result;
    }
}