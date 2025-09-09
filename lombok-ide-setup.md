# Lombok IDE 配置指南

## IntelliJ IDEA 配置

1. **安装Lombok插件**：
   - File → Settings → Plugins
   - 搜索"Lombok"并安装
   - 重启IDE

2. **启用注解处理**：
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - 勾选"Enable annotation processing"
   - 勾选"Obtain processors from project classpath"

3. **确认Lombok版本兼容性**：
   - 当前项目使用：Lombok 1.18.30
   - 确保IDE插件版本与之兼容

## Eclipse 配置

1. **下载lombok.jar**：
   - 从Maven仓库下载 lombok-1.18.30.jar

2. **安装到Eclipse**：
   - 双击 lombok.jar 文件运行安装程序
   - 或者手动添加到eclipse.ini: -javaagent:lombok.jar

3. **重启Eclipse**

## VS Code 配置

1. **安装扩展**：
   - "Language Support for Java(TM) by Red Hat"
   - 确保启用了注解处理

2. **工作区设置**：
   ```json
   {
     "java.compile.nullAnalysis.mode": "automatic",
     "java.configuration.updateBuildConfiguration": "interactive"
   }
   ```

## 验证配置

运行以下命令验证配置是否正确：

```bash
# 清理并重新编译
mvn clean compile

# 检查生成的getter/setter方法
javap -p "target/classes/com/cmict/internalpaas/model/Application.class" | grep -E "get|set"
```

如果仍有问题，尝试：

1. **清理IDE缓存**（IntelliJ: File → Invalidate Caches and Restart）
2. **重新导入Maven项目**
3. **确保JDK版本匹配**（项目使用JDK 11）