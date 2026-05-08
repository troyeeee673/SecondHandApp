package com.example.jianlou.staticVar;

import android.content.Context;
import android.content.SharedPreferences;

public class StaticVar {

    // 常量
    private static final String KEY_IS_LOGIN = "is_login";
    private static final String rootUrl = "http://192.168.93.1:8080";
    public static final String publishUrl = rootUrl + "/publish/";
    public static final String userUrl = rootUrl + "/user/login/";
    public static final String registerUrl = rootUrl + "/user/register/";
    public static final String editNameUrl = rootUrl + "/user/editname/";
    public static final String friendUrl = rootUrl + "/chat/friends";
    public static final String chatUrl = rootUrl + "/chat/";
    public static final String pushUrl = rootUrl + "/chat/push";
    public static String unreadCountUrl = rootUrl + "/chat/unread/count";
    public static final String indexUrl = rootUrl + "/index/";
    public static final String searchUrl = rootUrl + "/search/";
    public static final String detailUrl = rootUrl + "/goods/detail/";
    public static final String MyUrl = rootUrl + "/mygoods/";
    public static final String BaseUrl = "http://192.168.93.1:8080";

    public static String fileName = "session";
    public static String fileCookiename = "cookie";
    public static String fileUserName = "user_name";
    public static String fileHeadUrl = "head_url";
    public static String fileUserType = "user_type";
    public static String fileUserStatus = "user_status";
    public static String fileAccount = "account";
    // 用户账号在SharedPreferences中的key
    public static String fileUserAccount = "user_account";

    public static String cookie = "";
    public static String user_name = "";

    public static String user_type = "";
    public static String user_status = "";
    public static String headUrl = "";
    // 当前登录账号
    public static String account = "";
    public static String user_account = "";  // 用户账号

    public static final int LOGIN = 1;
    public static final int PublishmoneyNUM = 2;
    public static final int PublishClassify = 3;
    public static final String DeleteUrl = "http://192.168.93.1:8080/delete_goods/";

    // 帖子发布请求码
    public static final int POST_PUBLISH_REQUEST = 1002;
    public static final int PublishPostTypeNUM = 10;
    public static final int PublishPostCommunityNUM = 11;
    // 帖子发布接口
    public static String postPublishUrl = rootUrl + "/post/publish/";
    // 帖子查询接口
    public static String postGetAllUrl = rootUrl + "/post/get_all/";

    // 用户状态常量
    public static final String USER_STATUS_PENDING = "pending";
    public static final String USER_STATUS_APPROVED = "approved";
    public static final String USER_STATUS_REJECTED = "rejected";
    public static final String USER_STATUS_BANNED = "banned";
    // 用户类型常量
    public static final String USER_TYPE_USER = "user";
    public static final String USER_TYPE_ADMIN = "admin";


    // 新增等级变量
    public static String userLevel = "青铜";
    public static int userLevelScore = 0;

    // 保存用户信息
    public static void saveUserInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(fileCookiename, cookie);
        editor.putString(fileUserName, user_name);
        editor.putString(fileUserType, user_type);
        editor.putString(fileUserStatus, user_status);
        editor.putString(fileUserAccount, user_account);  // 保存用户账号
        editor.putString(fileAccount, account);
        editor.putString(fileHeadUrl, headUrl);
        editor.putBoolean(KEY_IS_LOGIN, true);
        editor.apply();
    }

    // 清空用户信息
    public static void clearUserInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
        cookie = "";
        user_name = "";
        user_type = "";
        user_status = "";
        account = "";
        headUrl = "";
        user_account = "";  // 清空用户账号
        userLevel = "青铜";  // 重置等级
        userLevelScore = 0; // 重置分数
    }

    // 加载用户信息
    public static void loadUserInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        boolean isLogin = sp.getBoolean(KEY_IS_LOGIN, false);
        if (isLogin) {
            cookie = sp.getString(fileCookiename, "");
            user_name = sp.getString(fileUserName, "");
            user_type = sp.getString(fileUserType, "");
            user_status = sp.getString(fileUserStatus, "");
            user_account = sp.getString(fileUserAccount, "");  // 加载用户账号
            account = sp.getString(fileAccount, "");
            headUrl = sp.getString(fileHeadUrl, "");
        } else {
            // 未登录时强制清空
            cookie = "";
            user_name = "";
            user_type = "";
            user_status = "";
            account = "";
            headUrl = "";
            user_account = "";
            userLevel = "青铜";
            userLevelScore = 0;
        }
    }

    // 判断是否登录
    public static boolean isLogin(Context context) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        boolean isLoginInSp = sp.getBoolean(KEY_IS_LOGIN, false);

        // 如果SP中记录已登录，但cookie为空，说明状态不一致，强制清理
        if (isLoginInSp && cookie.isEmpty()) {
            clearUserInfo(context);
            return false;
        }

        // 如果cookie不为空但SP中未记录登录，说明需要同步状态
        if (!cookie.isEmpty() && !isLoginInSp) {
            saveUserInfo(context);  // 自动同步到SP
        }

        // 最终判断：SP中记录已登录且cookie不为空
        return isLoginInSp && !cookie.isEmpty();
    }

    // 判断是否是管理员（简化版）
    public static boolean isAdmin(Context context) {
        if (!isLogin(context)) {
            return false;
        }
        // 直接从SP读取，不需要重复调用loadUserInfo
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        String userType = sp.getString(fileUserType, "");
        return USER_TYPE_ADMIN.equals(userType);
    }

    // 判断是否是已审核的普通用户
    public static boolean isApprovedUser(Context context) {
        if (!isLogin(context)) {
            return false;
        }
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        String userType = sp.getString(fileUserType, "");
        String userStatus = sp.getString(fileUserStatus, "");
        return USER_TYPE_USER.equals(userType) && USER_STATUS_APPROVED.equals(userStatus);
    }

    // 退出登录
    public static void logout(Context context) {
        clearUserInfo(context);
    }

    // 新增：保存用户等级信息
    public static void saveUserLevelInfo(Context context, String level, int score) {
        userLevel = level;
        userLevelScore = score;
        // 可以将等级信息也保存到SharedPreferences中
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("user_level", level);
        editor.putInt("user_level_score", score);
        editor.apply();
    }

    // 新增：加载用户等级信息
    public static void loadUserLevelInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        userLevel = sp.getString("user_level", "青铜");
        userLevelScore = sp.getInt("user_level_score", 0);
    }

}