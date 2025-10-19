    /**
     * ===== Tab版本专用方法 =====
     */

    /**
     * 开始自动扫描 (Tab版本)
     * 带4步进度动画的自动扫描功能
     */
    private async startAutoScan() {
        console.log("Starting auto-scan with progress animation...");

        const scanningStatus = this.modal?.querySelector<HTMLDivElement>("#scanningStatus");
        const scanResult = this.modal?.querySelector<HTMLDivElement>("#scanResult");
        const progressFill = this.modal?.querySelector<HTMLDivElement>("#progressFill");

        if (!scanningStatus || !scanResult) {
            console.error("Scan status elements not found");
            return;
        }

        try {
            // 显示扫描状态
            scanningStatus.classList.add("active");
            scanResult.classList.remove("active");

            // 定义4步进度
            const steps = [
                { id: "step1", progress: 25, delay: 500 },
                { id: "step2", progress: 50, delay: 1000 },
                { id: "step3", progress: 75, delay: 1500 },
                { id: "step4", progress: 100, delay: 2000 }
            ];

            // 调用后端API开始扫描
            const response = await fetch("/api/ssh-config-import/scan-local", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                }
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                if (response.status === 404) {
                    throw new Error("未找到本地SSH配置文件");
                }
                throw new Error(errorData?.message || `扫描失败：HTTP ${response.status}`);
            }

            const result: SSHConfigParseResult = await response.json();

            // 执行进度动画
            for (let i = 0; i < steps.length; i++) {
                await new Promise(resolve => setTimeout(resolve, steps[i].delay));

                const stepEl = this.modal?.querySelector<HTMLDivElement>(`#${steps[i].id}`);
                if (stepEl) {
                    stepEl.classList.add("active");
                    if (progressFill) {
                        progressFill.style.width = `${steps[i].progress}%`;
                    }
                }

                // 标记前一步完成
                if (i > 0) {
                    const prevStep = this.modal?.querySelector<HTMLDivElement>(`#${steps[i - 1].id}`);
                    if (prevStep) {
                        prevStep.classList.remove("active");
                        prevStep.classList.add("completed");
                        const icon = prevStep.querySelector(".scan-step-icon");
                        if (icon) icon.textContent = "✓";
                    }
                }
            }

            // 标记最后一步完成
            const lastStep = this.modal?.querySelector<HTMLDivElement>("#step4");
            if (lastStep) {
                lastStep.classList.remove("active");
                lastStep.classList.add("completed");
                const icon = lastStep.querySelector(".scan-step-icon");
                if (icon) icon.textContent = "✓";
            }

            // 延迟后显示结果
            await new Promise(resolve => setTimeout(resolve, 500));
            scanningStatus.classList.remove("active");
            scanResult.classList.add("active");

            // 处理扫描结果
            if (result.servers && result.servers.length > 0) {
                this.servers = result.servers.map(server => ({
                    ...server,
                    selected: server.valid && !server.duplicate,
                    editing: false
                }));
                this.updateSelectedServers();
                this.displayScanResult(result);
                showSuccess(`扫描完成！发现 ${result.totalHosts} 个配置，${this.servers.filter(s => s.valid && !s.duplicate).length} 个可导入`);
            } else {
                showError("未发现SSH配置文件");
            }

        } catch (error) {
            console.error("Auto-scan failed:", error);
            scanningStatus.classList.remove("active");
            showError(error instanceof Error ? error.message : "自动扫描失败");
        }
    }

    /**
     * 显示扫描结果 (Tab版本)
     */
    private displayScanResult(result: SSHConfigParseResult) {
        const resultInfo = this.modal?.querySelector<HTMLDivElement>("#scanResultInfo");
        const subtitle = this.modal?.querySelector<HTMLParagraphElement>("#scanResultSubtitle");
        const tipContent = this.modal?.querySelector<HTMLSpanElement>("#scanTipContent");

        if (!resultInfo) return;

        // 更新副标题
        if (subtitle) {
            const clientCount = new Set(result.servers.map(s => this.guessClientName(s.hostname || s.name))).size;
            subtitle.textContent = `检测到 ${clientCount} 个已安装的SSH客户端`;
        }

        // 填充结果信息
        const clientSummary = this.groupByClient(result.servers);
        resultInfo.innerHTML = clientSummary.map(item => `
            <div class="info-item">
                <div class="info-label">
                    ${this.getClientIcon(item.clientName)} ${item.clientName} ${item.version}
                    <span class="badge badge-success">已安装</span>
                </div>
                <div class="info-value">
                    ${item.sessionCount} 个会话 • ${item.configPath}
                </div>
            </div>
        `).join("");

        // 更新提示
        if (tipContent) {
            const totalSessions = result.servers.length;
            tipContent.textContent = `共发现 ${totalSessions} 个会话配置（来自${clientSummary.length}个客户端），系统将自动去重和合并。预计导入时间 3-5 分钟。`;
        }
    }

    /**
     * 根据hostname猜测客户端名称
     */
    private guessClientName(hostname: string): string {
        if (!hostname) return "Unknown";
        const lower = hostname.toLowerCase();
        if (lower.includes("securecrt")) return "SecureCRT";
        if (lower.includes("xshell")) return "Xshell";
        if (lower.includes("tabby")) return "Tabby";
        if (lower.includes("mobaxterm")) return "MobaXterm";
        if (lower.includes("putty")) return "PuTTY";
        return "SSH Config";
    }

    /**
     * 获取客户端图标
     */
    private getClientIcon(clientName: string): string {
        const icons: Record<string, string> = {
            "SecureCRT": "🔐",
            "Xshell": "📡",
            "Tabby": "⚡",
            "MobaXterm": "🖥️",
            "PuTTY": "🔧",
            "SSH Config": "💻",
            "Unknown": "💻"
        };
        return icons[clientName] || "💻";
    }

    /**
     * 按客户端分组
     */
    private groupByClient(servers: ServerImportPreview[]): {
        clientName: string;
        version: string;
        sessionCount: number;
        configPath: string;
    }[] {
        const grouped = new Map<string, {
            clientName: string;
            version: string;
            sessionCount: number;
            configPath: string;
        }>();

        servers.forEach(server => {
            const clientName = this.guessClientName(server.hostname || server.name);
            if (!grouped.has(clientName)) {
                grouped.set(clientName, {
                    clientName,
                    version: "未知版本",
                    sessionCount: 0,
                    configPath: server.hostname || "默认路径"
                });
            }
            grouped.get(clientName)!.sessionCount++;
        });

        return Array.from(grouped.values());
    }

    /**
     * 确认导入 (Tab版本)
     */
    private async confirmImport() {
        if (this.selectedServers.length === 0) {
            showError("请先选择要导入的服务器");
            return;
        }

        console.log(`Confirming import of ${this.selectedServers.length} servers...`);

        try {
            const response = await fetch("/api/ssh-config-import/confirm", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ servers: this.selectedServers })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                throw new Error(errorData?.message || `导入失败：HTTP ${response.status}`);
            }

            const result: ServerImportResult = await response.json();
            this.importResult = result;

            showSuccess(`导入成功！成功 ${result.successCount} 个，失败 ${result.failedCount} 个`);

            // 关闭模态框并触发事件
            this.close();
            eventBus.emit("server:imported", result);

        } catch (error) {
            console.error("Import failed:", error);
            showError(error instanceof Error ? error.message : "导入失败");
        }
    }
