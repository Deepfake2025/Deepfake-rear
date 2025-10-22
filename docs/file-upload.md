### 阿里云oss
用户头像，检测视频等资源上传到需要上传到阿里云oss实例中

### 上传流程
#### 1. 前端申请sts临时密钥
1. 前端向后端（本项目）申请sts临时密钥
2. 后端从环境变量中拿到ram用户的ACCESS_KEY_ID和ACCESS_KEY_SECRET，以及角色arn：RAM_ROLE_ARN
3. 后端向sts服务申请得到临时访问密钥
4. 后端将临时密钥返回给前端

```java
// 申请sts临时访问凭证示例
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
public class StsServiceSample {
    public static void main(String[] args) { 
        // STS服务接入点，例如sts.cn-hangzhou.aliyuncs.com。您可以通过公网或者VPC接入STS服务。       
        String endpoint = "sts.cn-guangzhou.aliyuncs.com";
        // 从环境变量中获取步骤1.1生成的RAM用户的访问密钥（AccessKey ID和AccessKey Secret）。
        String accessKeyId = System.getenv("ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("ACCESS_KEY_SECRET");
        // 从环境变量中获取步骤1.3生成的RAM角色的RamRoleArn。
        String roleArn = System.getenv("RAM_ROLE_ARN");
        // 自定义角色会话名称，用来区分不同的令牌，例如可填写为SessionTest。        
        String roleSessionName = "SessionTest";   
        // 临时访问凭证将获得角色拥有的所有权限。      
        String policy = null;
        // 临时访问凭证的有效时间，单位为秒。最小值为900，最大值以当前角色设定的最大会话时间为准。当前角色最大会话时间取值范围为3600秒~43200秒，默认值为3600秒。
        // 在上传大文件或者其他较耗时的使用场景中，建议合理设置临时访问凭证的有效时间，确保在完成目标任务前无需反复调用STS服务以获取临时访问凭证。
        Long durationSeconds = 900L;
        try {
            // 发起STS请求所在的地域。建议保留默认值，默认值为空字符串（""）。
            String regionId = "";
            // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
            DefaultProfile.addEndpoint(regionId, "Sts", endpoint);
            // 添加endpoint。适用于Java SDK 3.12.0以下版本。
            // DefaultProfile.addEndpoint("",regionId, "Sts", endpoint);
            // 构造default profile。
            IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            // 构造client。
            DefaultAcsClient client = new DefaultAcsClient(profile);
            final AssumeRoleRequest request = new AssumeRoleRequest();
            // 适用于Java SDK 3.12.0及以上版本。
            request.setSysMethod(MethodType.POST);
            // 适用于Java SDK 3.12.0以下版本。
            // request.setMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName(roleSessionName);
            request.setPolicy(policy); 
            request.setDurationSeconds(durationSeconds); 
            final AssumeRoleResponse response = client.getAcsResponse(request);
            System.out.println("Expiration: " + response.getCredentials().getExpiration());
            System.out.println("Access Key Id: " + response.getCredentials().getAccessKeyId());
            System.out.println("Access Key Secret: " + response.getCredentials().getAccessKeySecret());
            System.out.println("Security Token: " + response.getCredentials().getSecurityToken());
            System.out.println("RequestId: " + response.getRequestId());
        } catch (ClientException e) {
            System.out.println("Failed：");
            System.out.println("Error code: " + e.getErrCode());
            System.out.println("Error message: " + e.getErrMsg());
            System.out.println("RequestId: " + e.getRequestId());
        }
    }
}
```


#### 2. 前端使用sts上传文件
- 略

#### 3. 前端调用后端回调函数
- 后端需要提供毁掉接口，由oss服务触发



### 跨域问题
后端将sts返回给前端后，前端会直接与oss服务进行通信，会遇到跨域问题。所以需要在后端初始化时，检查oss 的bucket是否以及配置好了cors

```java
// CORS示例代码

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.SetBucketCORSRequest;
import java.util.ArrayList;

public class Demo {

    public static void main(String[] args) throws Exception {
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = "https://oss-cn-guangzhou.aliyuncs.com";
        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        // 填写Bucket名称，例如examplebucket。
        String bucketName = "examplebucket";
        // 填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
        String region = "cn-guangzhou";

        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);        
        OSS ossClient = OSSClientBuilder.create()
        .endpoint(endpoint)
        .credentialsProvider(credentialsProvider)
        .clientConfiguration(clientBuilderConfiguration)
        .region(region)               
        .build();

        try {
            SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);

            // 每个存储空间最多允许设置10条跨域规则。
            ArrayList<SetBucketCORSRequest.CORSRule> putCorsRules = new ArrayList<SetBucketCORSRequest.CORSRule>();

            SetBucketCORSRequest.CORSRule corRule = new SetBucketCORSRequest.CORSRule();

            ArrayList<String> allowedOrigin = new ArrayList<String>();
            // 指定允许跨域请求的来源。
            allowedOrigin.add( "http://example.com");

            ArrayList<String> allowedMethod = new ArrayList<String>();
            // 指定允许的跨域请求方法(GET/PUT/DELETE/POST/HEAD)。
            allowedMethod.add("GET");

            ArrayList<String> allowedHeader = new ArrayList<String>();
            // 是否允许预取指令（OPTIONS）中Access-Control-Request-Headers头中指定的Header。
            allowedHeader.add("x-oss-test");

            ArrayList<String> exposedHeader = new ArrayList<String>();
            // 指定允许用户从应用程序中访问的响应头。
            exposedHeader.add("x-oss-test1");
            // AllowedOrigins和AllowedMethods最多支持一个星号（*）通配符。星号（*）表示允许所有的域来源或者操作。
            corRule.setAllowedMethods(allowedMethod);
            corRule.setAllowedOrigins(allowedOrigin);
            // AllowedHeaders和ExposeHeaders不支持通配符。
            corRule.setAllowedHeaders(allowedHeader);
            corRule.setExposeHeaders(exposedHeader);
            // 指定浏览器对特定资源的预取（OPTIONS）请求返回结果的缓存时间，单位为秒。
            corRule.setMaxAgeSeconds(10);

            // 最多允许10条规则。
            putCorsRules.add(corRule);
            // 已存在的规则将被覆盖。
            request.setCorsRules(putCorsRules);
            // 指定是否返回Vary: Origin头。指定为TRUE，表示不管发送的是否为跨域请求或跨域请求是否成功，均会返回Vary: Origin头。指定为False，表示任何情况下都不会返回Vary: Origin头。
            // request.setResponseVary(Boolean.TRUE);
            ossClient.setBucketCORS(request);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
```



### 需要实现的功能和涉及到的接口
#### 上传头像
- 描述：将头像图片上传到oss，更新mysql中的xx_user表
- 接口：
  - /user/avatar-upload/init - 检查文件元数据，返回STS凭证（包含文件类型、大小限制）
  - /user/avatar-upload/callback - OSS上传完成回调（文件信息验证、数据库更新）
  - /user/avatar-upload/confirm - 前端最终确认接口（事务一致性保证）




### 阿里云资源访问工具类架构

#### 1. 整体架构设计

```
com.deepfake.cloud
├── config/
│   ├── AliyunProperties.java          # 阿里云配置属性
│   └── AliyunCloudConfig.java         # 自动配置类
├── service/
│   ├── StsService.java                # STS临时凭证服务
│   ├── OssService.java                # OSS操作服务
│   └── CloudStorageService.java       # 云存储统一接口
├── model/
│   ├── StsCredentials.java            # STS凭证模型
│   ├── UploadPolicy.java              # 上传策略模型
│   └── FileUploadResult.java          # 文件上传结果模型
├── exception/
│   ├── CloudStorageException.java     # 云存储异常基类
│   ├── StsException.java              # STS相关异常
│   └── OssException.java              # OSS相关异常
└── util/
    ├── OssUtils.java                  # OSS工具类
    └── PolicyBuilder.java             # 权限策略构建器
```

#### 2. 配置管理

```java
// AliyunProperties.java
@ConfigurationProperties(prefix = "aliyun")
@Data
public class AliyunProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String regionId = "cn-guangzhou";
    private String roleArn;
    private Oss oss = new Oss();

    @Data
    public static class Oss {
        private String bucketName;
        private String endpoint;
        private String callbackUrl;
        private Long maxFileSize = 10 * 1024 * 1024L; // 10MB
        private List<String> allowedFileTypes = Arrays.asList("jpg", "jpeg", "png", "gif");
        private String avatarPath = "user/avatars/";
        private String videoPath = "user/videos/";
    }
}
```

#### 3. STS服务封装

```java
// StsService.java
@Service
@Slf4j
public class StsService {

    @Autowired
    private AliyunProperties aliyunProperties;

    private DefaultAcsClient stsClient;

    @PostConstruct
    public void init() {
        try {
            IClientProfile profile = DefaultProfile.getProfile(
                aliyunProperties.getRegionId(),
                aliyunProperties.getAccessKeyId(),
                aliyunProperties.getAccessKeySecret()
            );
            stsClient = new DefaultAcsClient(profile);
        } catch (ClientException e) {
            log.error("初始化STS客户端失败", e);
            throw new StsException("STS客户端初始化失败", e);
        }
    }

    /**
     * 获取文件上传STS临时凭证
     */
    public StsCredentials getUploadCredentials(String fileType, String userId) {
        try {
            // 构建精细化的权限策略
            String policy = PolicyBuilder.buildUploadPolicy(
                aliyunProperties.getOss().getBucketName(),
                aliyunProperties.getOss().getAvatarPath(),
                fileType,
                userId
            );

            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setRoleArn(aliyunProperties.getRoleArn());
            request.setRoleSessionName("upload-session-" + userId + "-" + System.currentTimeMillis());
            request.setPolicy(policy);
            request.setDurationSeconds(3600L); // 1小时有效期

            AssumeRoleResponse response = stsClient.getAcsResponse(request);

            return StsCredentials.builder()
                .accessKeyId(response.getCredentials().getAccessKeyId())
                .accessKeySecret(response.getCredentials().getAccessKeySecret())
                .securityToken(response.getCredentials().getSecurityToken())
                .expiration(response.getCredentials().getExpiration())
                .bucketName(aliyunProperties.getOss().getBucketName())
                .region(aliyunProperties.getRegionId())
                .endpoint(aliyunProperties.getOss().getEndpoint())
                .build();

        } catch (ClientException e) {
            log.error("获取STS临时凭证失败", e);
            throw new StsException("获取STS临时凭证失败: " + e.getErrMsg(), e);
        }
    }
}
```

#### 4. OSS服务封装

```java
// OssService.java
@Service
@Slf4j
public class OssService {

    @Autowired
    private AliyunProperties aliyunProperties;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        ClientBuilderConfiguration config = new ClientBuilderConfiguration();
        config.setSignatureVersion(SignVersion.V4);

        ossClient = OSSClientBuilder.create()
            .endpoint(aliyunProperties.getOss().getEndpoint())
            .credentialsProvider(CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider())
            .clientConfiguration(config)
            .region(aliyunProperties.getRegionId())
            .build();

        // 初始化CORS配置
        initCorsConfiguration();
    }

    /**
     * 初始化CORS配置
     */
    private void initCorsConfiguration() {
        try {
            SetBucketCORSRequest request = new SetBucketCORSRequest(aliyunProperties.getOss().getBucketName());

            SetBucketCORSRequest.CORSRule corsRule = new SetBucketCORSRequest.CORSRule();
            corsRule.setAllowedMethods(Arrays.asList("GET", "PUT", "POST", "DELETE", "HEAD"));
            corsRule.setAllowedOrigins(Arrays.asList("*"));
            corsRule.setAllowedHeaders(Arrays.asList("*"));
            corsRule.setExposeHeaders(Arrays.asList("ETag", "x-oss-request-id"));
            corsRule.setMaxAgeSeconds(3600);

            request.setCorsRules(Arrays.asList(corsRule));
            ossClient.setBucketCORS(request);

            log.info("OSS CORS配置初始化成功");
        } catch (OSSException | ClientException e) {
            log.error("OSS CORS配置初始化失败", e);
            throw new OssException("CORS配置初始化失败", e);
        }
    }

    /**
     * 验证上传回调
     */
    public boolean validateCallback(String authorization, String body, String path) {
        try {
            // 验证OSS回调签名
            String expectedAuth = OssUtils.calculateSignature(
                aliyunProperties.getAccessKeySecret(),
                path + body
            );

            return expectedAuth.equals(authorization);
        } catch (Exception e) {
            log.error("回调验证失败", e);
            return false;
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        try {
            ossClient.deleteObject(aliyunProperties.getOss().getBucketName(), objectName);
            log.info("文件删除成功: {}", objectName);
        } catch (OSSException | ClientException e) {
            log.error("文件删除失败: {}", objectName, e);
            throw new OssException("文件删除失败: " + objectName, e);
        }
    }
}
```

#### 5. 云存储统一接口

```java
// CloudStorageService.java
@Service
@Slf4j
public class CloudStorageService {

    @Autowired
    private StsService stsService;

    @Autowired
    private OssService ossService;

    @Autowired
    private AliyunProperties aliyunProperties;

    /**
     * 获取头像上传凭证
     */
    public StsCredentials getAvatarUploadCredentials(Long userId) {
        validateUser(userId);
        validateFileType("avatar");

        String objectPath = aliyunProperties.getOss().getAvatarPath() +
                           userId + "/" + generateFileName("avatar");

        StsCredentials credentials = stsService.getUploadCredentials("image", userId.toString());
        credentials.setObjectPath(objectPath);

        return credentials;
    }

    /**
     * 处理上传完成回调
     */
    public FileUploadResult handleUploadCallback(String callbackBody) {
        try {
            // 解析回调数据
            Map<String, String> callbackData = OssUtils.parseCallbackBody(callbackBody);

            String objectName = callbackData.get("object");
            String fileSize = callbackData.get("size");
            String mimeType = callbackData.get("mimeType");

            // 验证文件信息
            validateUploadedFile(objectName, fileSize, mimeType);

            return FileUploadResult.builder()
                .objectName(objectName)
                .fileSize(Long.parseLong(fileSize))
                .mimeType(mimeType)
                .url(buildFileUrl(objectName))
                .uploadTime(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("处理上传回调失败", e);
            throw new CloudStorageException("处理上传回调失败", e);
        }
    }

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CloudStorageException("无效的用户ID");
        }
    }

    private void validateFileType(String fileType) {
        List<String> allowedTypes = aliyunProperties.getOss().getAllowedFileTypes();
        // 实现文件类型验证逻辑
    }

    private String generateFileName(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" +
               UUID.randomUUID().toString().substring(0, 8);
    }

    private String buildFileUrl(String objectName) {
        return "https://" + aliyunProperties.getOss().getBucketName() +
               "." + aliyunProperties.getOss().getEndpoint().replace("https://", "") +
               "/" + objectName;
    }
}
```

#### 6. 权限策略构建器

```java
// PolicyBuilder.java
public class PolicyBuilder {

    /**
     * 构建文件上传权限策略
     */
    public static String buildUploadPolicy(String bucketName, String pathPrefix,
                                         String fileType, String userId) {
        return "{\n" +
            "  \"Version\": \"1\",\n" +
            "  \"Statement\": [\n" +
            "    {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Action\": [\n" +
            "        \"oss:PutObject\",\n" +
            "        \"oss:PutObjectAcl\"\n" +
            "      ],\n" +
            "      \"Resource\": [\n" +
            "        \"acs:oss:*:*:" + bucketName + "/" + pathPrefix + userId + "/*\"\n" +
            "      ],\n" +
            "      \"Condition\": {\n" +
            "        \"StringLike\": {\n" +
            "          \"oss:x-oss-object-name\": \"" + pathPrefix + userId + "/*\",\n" +
            "          \"oss:x-oss-content-type\": \"image/*\"\n" +
            "        },\n" +
            "        \"NumericLessThanEquals\": {\n" +
            "          \"oss:x-oss-content-length\": 10485760\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
```

#### 7. 异常处理

```java
// CloudStorageException.java
public class CloudStorageException extends RuntimeException {
    private String errorCode;

    public CloudStorageException(String message) {
        super(message);
    }

    public CloudStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudStorageException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// StsException.java
public class StsException extends CloudStorageException {
    public StsException(String message) {
        super("STS_ERROR", message);
    }

    public StsException(String message, Throwable cause) {
        super("STS_ERROR", message);
        initCause(cause);
    }
}

// OssException.java
public class OssException extends CloudStorageException {
    public OssException(String message) {
        super("OSS_ERROR", message);
    }

    public OssException(String message, Throwable cause) {
        super("OSS_ERROR", message);
        initCause(cause);
    }
}
```

### 后续优化的点
1. ram角色使用更精细的权限策略
2. 实现多云厂商支持（AWS S3、腾讯云COS等）
3. 添加文件内容安全检测（病毒扫描、图片识别）
4. 实现断点续传和分片上传功能
5. 添加上传进度监控和统计功能
6. 实现文件预签名URL生成，支持临时访问授权