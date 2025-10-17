package com.cmict.internalpaas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 基础应用启动测试
 * 
 * Phase4-Step4 更新 (2025-10-17):
 * - 使用 H2 内存数据库 (scope=test) 支持非监控实体
 * - 监控数据由 Metrics Hub 管理
 */
@SpringBootTest
@ActiveProfiles("test")
class InternalpaasApplicationTests {

	@Test
	void contextLoads() {
		// 验证应用上下文可以正常加载
	}

}
