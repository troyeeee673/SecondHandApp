#### **一、数据库连接（以管理员身份打开cmd）**

（1）进入mysql的安装路径

D:

cd D:\\softwares\\MySQL\\mysql-8.4.4-winx64（换成自己mysql的安装路径）



（2）开启数据库

net start mysql

mysql -u root -p    123456（换成自己的密码）



（3）进入jianlou数据库

USE jianlou;



（4）SHOW TABLES;  -- 列出当前数据库的所有表名



（5）DESC user;查看user表的结构



#### **二、后端项目（springboot+Thymeleaf模板引擎渲染前端）**

（1）直接在IntelliJ IDEA 中打开项目jianlou这个文件夹即可（jianlou这个文件夹下面有build.gradle这个文件）

（2）ctrl+shift+R替换成自己的ip

（3）修改application.yml文件中数据库的登录密码password和登录名username，确保这个里面配置的端口port:8080没有被占用

（4）build.gradle每次修改都要同步

（5）管理员端的访问链接为http://localhost:8080/admin/web/login





#### **三、前端Android项目**

（1）在AS当中打开project这个文件夹（project这个文件夹下面有build.gradle这个文件）

（2）大象同步：同步过程中首先会根据Gradle Wrapper的配置文件gradle-wrapper.properties来下载Gradle，distributionUrl这个变量Gradle 压缩包的存储路径，这个路径在本地电脑的地址为：C:\\Users\\LY\\.gradle\\wrapper\\dists。我用的是distributionUrl=https\\://services.gradle.org/distributions/gradle-6.7.1-all.zip。需要下载的依赖在build.gradle（Module:app）当中，每次修改这些依赖都要同步。

（3）ctrl+shift+R替换成自己的ip

（4）我们这个项目是JDK1.8，在File->Settings->Build,Execution,Deployment->Build Tools->Gradle下面进行配置。

（5）运行项目即可。



#### **四、支付宝配置**

（1）支付宝沙箱版下载

用自己手机上支付宝扫码登录

https://auth.alipay.com/login/index.htm?goto=https%3A%2F%2Fopen.alipay.com%2Fdevelop%2Fsandbox%2Ftool%2Falipayclint

（2）首先在沙箱工具中支付宝扫码获得下载连接，然后复制下载链接到自己手机的自带浏览器进行下载。下载完成后以“沙箱账号”当中的“买家信息”进行登录，学则账号登录，填写“买家账号”和“登陆密码”即可。

（3）com.example.jianlou.config.AlipayConfig四个变量修改：打开https://opendocs.alipay.com/common/02kipk，下载安装“支付宝开放平台密钥工具”，然后生成密钥，在沙箱应用中可以获得自己的APPID、绑定的商家账号（PID）。点击“自定义密钥”，先启动，然后查看，传过去“支付宝开放平台密钥工具”中生成的应用公钥，即可获得支付宝公钥。上面获得的四个信息替换掉后端com.example.jianlou.config.AlipayConfig中的APP\_ID，PID，RAW\_PRIVATE\_KEY，RAW\_PUBLIC\_KEY这四个变量

