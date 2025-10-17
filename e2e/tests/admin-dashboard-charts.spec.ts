import { test, expect } from '@playwright/test';

/**
 * P0-Day5: Admin Dashboard Charts E2E Test
 * 验证 Chart.js 图表在浏览器中正确渲染
 */

test.describe('Admin Dashboard Charts', () => {
    test.beforeEach(async ({ page }) => {
        // 1. 先访问登录页面
        await page.goto('http://localhost:9091/login');
        
        // 2. 填写登录表单 (使用默认测试账号)
        await page.fill('input[placeholder="请输入用户名"]', 'admin');
        await page.fill('input[placeholder="请输入密码"]', 'admin123');
        
        // 3. 点击登录按钮
        await page.click('button:has-text("登录")');
        
        // 4. 等待登录成功并跳转到工作台
        await page.waitForURL('**/admin/workspace', { timeout: 10000 });
        
        // 5. 等待 SPA 路由加载完成
        await page.waitForLoadState('networkidle');
        
        // 6. 等待 Chart.js 初始化完成
        await page.waitForTimeout(2000);
    });

    test('应该成功加载 3 个图表容器', async ({ page }) => {
        // 检查图表区域是否存在
        const chartsSection = page.locator('[data-dashboard-charts]');
        await expect(chartsSection).toBeVisible();

        // 检查 3 个 Canvas 元素
        const serverStatusChart = page.locator('#server-status-chart');
        const resourceUsageChart = page.locator('#resource-usage-chart');
        const realtimeMonitorChart = page.locator('#realtime-monitor-chart');

        await expect(serverStatusChart).toBeVisible();
        await expect(resourceUsageChart).toBeVisible();
        await expect(realtimeMonitorChart).toBeVisible();
    });

    test('Canvas 元素应该有正确的尺寸', async ({ page }) => {
        // 获取 Canvas 尺寸
        const canvas1Dimensions = await page.locator('#server-status-chart').evaluate((el) => ({
            width: el.offsetWidth,
            height: el.offsetHeight,
            canvasWidth: (el as HTMLCanvasElement).width,
            canvasHeight: (el as HTMLCanvasElement).height
        }));

        const canvas2Dimensions = await page.locator('#resource-usage-chart').evaluate((el) => ({
            width: el.offsetWidth,
            height: el.offsetHeight,
            canvasWidth: (el as HTMLCanvasElement).width,
            canvasHeight: (el as HTMLCanvasElement).height
        }));

        const canvas3Dimensions = await page.locator('#realtime-monitor-chart').evaluate((el) => ({
            width: el.offsetWidth,
            height: el.offsetHeight,
            canvasWidth: (el as HTMLCanvasElement).width,
            canvasHeight: (el as HTMLCanvasElement).height
        }));

        // 验证尺寸 > 0
        expect(canvas1Dimensions.width).toBeGreaterThan(0);
        expect(canvas1Dimensions.height).toBeGreaterThan(0);
        expect(canvas2Dimensions.width).toBeGreaterThan(0);
        expect(canvas2Dimensions.height).toBeGreaterThan(0);
        expect(canvas3Dimensions.width).toBeGreaterThan(0);
        expect(canvas3Dimensions.height).toBeGreaterThan(0);

        console.log('📊 Canvas 尺寸:', {
            serverStatus: canvas1Dimensions,
            resourceUsage: canvas2Dimensions,
            realtimeMonitor: canvas3Dimensions
        });
    });

    test('应该看到 Chart.js 初始化日志', async ({ page }) => {
        const logs: string[] = [];
        
        // 捕获控制台日志
        page.on('console', msg => {
            const text = msg.text();
            if (text.includes('[Dashboard]') || text.includes('[SPA]')) {
                logs.push(text);
            }
        });

        // 刷新页面触发初始化
        await page.reload();
        await page.waitForTimeout(3000);

        // 验证关键日志存在
        const hasInitLog = logs.some(log => log.includes('开始初始化图表'));
        const hasCreatedLog = logs.some(log => log.includes('图表已创建'));
        const hasCompleteLog = logs.some(log => log.includes('图表初始化完成'));

        expect(hasInitLog).toBeTruthy();
        expect(hasCreatedLog).toBeTruthy();
        expect(hasCompleteLog).toBeTruthy();

        console.log('📝 捕获到的日志:', logs);
    });

    test('应该能够验证 Chart.js 实例存在', async ({ page }) => {
        // 检查全局 dashboard 对象
        const dashboardExists = await page.evaluate(() => {
            return typeof (window as any).dashboard !== 'undefined';
        });

        expect(dashboardExists).toBeTruthy();

        // 检查图表实例数量
        const chartsCount = await page.evaluate(() => {
            const dashboard = (window as any).dashboard;
            return dashboard?.getCharts?.()?.size || 0;
        });

        expect(chartsCount).toBe(3);
        console.log('✅ 图表实例数量:', chartsCount);
    });

    test('应该能够截图保存图表效果', async ({ page }) => {
        // 等待图表渲染完成
        await page.waitForTimeout(2000);

        // 截取整个图表区域
        const chartsSection = page.locator('[data-dashboard-charts]');
        await chartsSection.screenshot({
            path: 'e2e/screenshots/dashboard-charts.png'
        });

        // 单独截取每个图表
        await page.locator('#server-status-chart').screenshot({
            path: 'e2e/screenshots/chart-server-status.png'
        });

        await page.locator('#resource-usage-chart').screenshot({
            path: 'e2e/screenshots/chart-resource-usage.png'
        });

        await page.locator('#realtime-monitor-chart').screenshot({
            path: 'e2e/screenshots/chart-realtime-monitor.png'
        });

        console.log('📸 截图已保存到 e2e/screenshots/');
    });

    test('Canvas 应该有可见的像素内容 (验证图表真实渲染)', async ({ page }) => {
        // 检查 Canvas 是否有实际绘制内容
        const hasContent = await page.locator('#server-status-chart').evaluate((canvas) => {
            const ctx = (canvas as HTMLCanvasElement).getContext('2d');
            if (!ctx) return false;
            
            const imageData = ctx.getImageData(0, 0, canvas.offsetWidth, canvas.offsetHeight);
            const pixels = imageData.data;
            
            // 检查是否有非透明像素
            for (let i = 3; i < pixels.length; i += 4) {
                if (pixels[i] > 0) return true;
            }
            return false;
        });

        expect(hasContent).toBeTruthy();
        console.log('✅ Canvas 包含渲染内容');
    });
});
