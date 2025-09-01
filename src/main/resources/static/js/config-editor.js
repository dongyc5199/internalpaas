/**
 * Configuration Editor JavaScript
 * Advanced application configuration management with validation and real-time preview
 */

class ConfigurationEditor {
    constructor() {
        this.applicationId = null;
        this.csrfToken = null;
        this.csrfHeader = null;
        this.currentConfig = {};
        this.activeConfig = null;
        this.configHistory = [];
        this.templates = [];
        this.predefinedTemplates = {};
        this.validationErrors = {};
        this.isDirty = false;
        
        this.init();
    }

    init() {
        this.loadPageData();
        this.initializeEventListeners();
        this.loadActiveConfiguration();
        this.loadTemplates();
        this.loadConfigurationHistory();
        this.setupAutoValidation();
        this.setupEnvironmentVariableEditor();
    }

    loadPageData() {
        const pageData = document.getElementById('pageData');
        if (pageData) {
            this.applicationId = pageData.dataset.applicationId;
            this.csrfToken = pageData.dataset.csrfToken;
            this.csrfHeader = pageData.dataset.csrfHeader;
        }
    }

    initializeEventListeners() {
        // Form validation and change tracking
        document.getElementById('configForm').addEventListener('change', (e) => {
            this.markAsDirty();
            this.validateField(e.target);
            this.updatePreview();
        });

        document.getElementById('configForm').addEventListener('input', (e) => {
            this.markAsDirty();
            this.validateField(e.target);
            this.updatePortDisplays();
        });

        // Action buttons
        document.getElementById('saveConfigBtn').addEventListener('click', () => this.saveConfiguration());
        document.getElementById('applyConfigBtn').addEventListener('click', () => this.applyConfiguration());
        document.getElementById('resetConfigBtn').addEventListener('click', () => this.resetConfiguration());
        document.getElementById('validateConfigBtn').addEventListener('click', () => this.validateConfiguration());
        document.getElementById('previewConfigBtn').addEventListener('click', () => this.showConfigurationPreview());
        document.getElementById('previewConfigBtn2').addEventListener('click', () => this.showConfigurationPreview());
        document.getElementById('hotReloadBtn').addEventListener('click', () => this.hotReloadConfiguration());

        // Template and import/export buttons
        document.getElementById('loadTemplateBtn').addEventListener('click', () => this.showTemplateModal());
        document.getElementById('importConfigBtn').addEventListener('click', () => this.showImportModal());
        document.getElementById('exportConfigBtn').addEventListener('click', () => this.showExportModal());
        document.getElementById('createBackupBtn').addEventListener('click', () => this.createBackup());
        document.getElementById('saveAsTemplateBtn').addEventListener('click', () => this.showSaveTemplateModal());
        document.getElementById('compareConfigBtn').addEventListener('click', () => this.showCompareModal());

        // Modal confirmations
        document.getElementById('importConfirmBtn').addEventListener('click', () => this.importConfiguration());
        document.getElementById('exportConfirmBtn').addEventListener('click', () => this.exportConfiguration());
        document.getElementById('saveTemplateConfirmBtn').addEventListener('click', () => this.saveTemplate());
        document.getElementById('compareConfirmBtn').addEventListener('click', () => this.compareConfigurations());

        // Environment variable management
        document.getElementById('addEnvVarBtn').addEventListener('click', () => this.addEnvironmentVariable());

        // Debug and JMX checkboxes
        document.getElementById('enableDebug').addEventListener('change', (e) => {
            this.toggleDebugOptions(e.target.checked);
        });

        document.getElementById('enableJmx').addEventListener('change', (e) => {
            this.toggleJmxOptions(e.target.checked);
        });

        // Hot reload support checkbox
        document.getElementById('hotReloadSupport').addEventListener('change', (e) => {
            this.toggleHotReloadButton(e.target.checked);
        });

        // Prevent accidental navigation away from unsaved changes
        window.addEventListener('beforeunload', (e) => {
            if (this.isDirty) {
                e.preventDefault();
                e.returnValue = '您有未保存的配置更改，确定要离开吗？';
            }
        });
    }

    async loadActiveConfiguration() {
        try {
            const response = await this.apiCall(`/api/applications/${this.applicationId}/config/active`);
            if (response.ok) {
                this.activeConfig = await response.json();
                this.populateForm(this.activeConfig);
                this.showSuccess('已加载活动配置');
            } else {
                this.showInfo('没有找到活动配置，使用默认值');
                this.loadDefaultConfiguration();
            }
        } catch (error) {
            console.error('Failed to load active configuration:', error);
            this.showError('加载配置失败');
            this.loadDefaultConfiguration();
        }
    }

    loadDefaultConfiguration() {
        // Load default configuration values
        const defaultConfig = {
            jvmOptions: '-Xms512m -Xmx1g',
            gcOptions: '-XX:+UseG1GC',
            systemProperties: '-Dspring.profiles.active=dev',
            serverPort: 8080,
            enableDebug: false,
            debugPort: 5005,
            enableJmx: false,
            jmxPort: 9999,
            activeProfiles: 'dev',
            maxThreads: 200,
            connectionTimeout: 30000
        };
        
        this.populateForm(defaultConfig);
    }

    async loadTemplates() {
        try {
            const response = await this.apiCall(`/api/applications/${this.applicationId}/config/templates`);
            if (response.ok) {
                const data = await response.json();
                this.templates = data.templates || [];
                this.predefinedTemplates = data.predefined || {};
                this.updateTemplateList();
            }
        } catch (error) {
            console.error('Failed to load templates:', error);
        }
    }

    async loadConfigurationHistory() {
        try {
            const response = await this.apiCall(`/api/applications/${this.applicationId}/config/history?limit=20`);
            if (response.ok) {
                this.configHistory = await response.json();
                this.updateHistoryList();
            }
        } catch (error) {
            console.error('Failed to load configuration history:', error);
        }
    }

    populateForm(config) {
        // JVM Configuration
        this.setFieldValue('jvmOptions', config.jvmOptions);
        this.setFieldValue('gcOptions', config.gcOptions);
        this.setFieldValue('systemProperties', config.systemProperties);
        this.setFieldValue('maxThreads', config.maxThreads);
        this.setFieldValue('connectionTimeout', config.connectionTimeout);
        this.setFieldValue('activeProfiles', config.activeProfiles);

        // Environment Variables
        this.setFieldValue('environmentVariables', config.environmentVariables);
        this.populateEnvironmentVariables(config.environmentVariables);

        // Spring Configuration
        this.setFieldValue('serverPort', config.serverPort);
        this.setFieldValue('serverContextPath', config.serverContextPath);
        this.setFieldValue('healthCheckPath', config.healthCheckPath);
        this.setFieldValue('healthCheckTimeout', config.healthCheckTimeout);
        this.setFieldValue('programArguments', config.programArguments);
        this.setFieldValue('springProperties', config.springProperties);
        this.setFieldValue('loggingConfig', config.loggingConfig);

        // Debug Configuration
        this.setFieldValue('enableDebug', config.enableDebug);
        this.setFieldValue('debugPort', config.debugPort);
        this.setFieldValue('debugOptions', config.debugOptions);
        this.toggleDebugOptions(config.enableDebug);

        // JMX Configuration
        this.setFieldValue('enableJmx', config.enableJmx);
        this.setFieldValue('jmxPort', config.jmxPort);
        this.setFieldValue('jmxAuthUser', config.jmxAuthUser);
        this.toggleJmxOptions(config.enableJmx);

        // Advanced Configuration
        this.setFieldValue('description', config.description);
        this.setFieldValue('configVersion', config.configVersion || '1.0.0');

        this.updatePortDisplays();
        this.currentConfig = this.getFormData();
        this.isDirty = false;
    }

    getFormData() {
        return {
            jvmOptions: this.getFieldValue('jvmOptions'),
            gcOptions: this.getFieldValue('gcOptions'),
            systemProperties: this.getFieldValue('systemProperties'),
            environmentVariables: this.getFieldValue('environmentVariables'),
            programArguments: this.getFieldValue('programArguments'),
            springProperties: this.getFieldValue('springProperties'),
            enableJmx: this.getFieldValue('enableJmx'),
            jmxPort: this.getFieldValue('jmxPort'),
            jmxAuthUser: this.getFieldValue('jmxAuthUser'),
            enableDebug: this.getFieldValue('enableDebug'),
            debugPort: this.getFieldValue('debugPort'),
            debugOptions: this.getFieldValue('debugOptions'),
            loggingConfig: this.getFieldValue('loggingConfig'),
            activeProfiles: this.getFieldValue('activeProfiles'),
            serverPort: this.getFieldValue('serverPort'),
            serverContextPath: this.getFieldValue('serverContextPath'),
            healthCheckPath: this.getFieldValue('healthCheckPath'),
            healthCheckTimeout: this.getFieldValue('healthCheckTimeout'),
            maxThreads: this.getFieldValue('maxThreads'),
            connectionTimeout: this.getFieldValue('connectionTimeout'),
            description: this.getFieldValue('description')
        };
    }

    setFieldValue(fieldId, value) {
        const field = document.getElementById(fieldId);
        if (field) {
            if (field.type === 'checkbox') {
                field.checked = Boolean(value);
            } else {
                field.value = value || '';
            }
        }
    }

    getFieldValue(fieldId) {
        const field = document.getElementById(fieldId);
        if (field) {
            if (field.type === 'checkbox') {
                return field.checked;
            } else if (field.type === 'number') {
                return field.value ? parseInt(field.value) : null;
            } else {
                return field.value;
            }
        }
        return null;
    }

    setupAutoValidation() {
        // Set up real-time validation for key fields
        const validationFields = ['jvmOptions', 'gcOptions', 'systemProperties', 'environmentVariables', 
                                'springProperties', 'serverPort', 'debugPort', 'jmxPort'];
        
        validationFields.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.addEventListener('blur', () => this.validateField(field));
                field.addEventListener('input', this.debounce(() => this.validateField(field), 500));
            }
        });
    }

    setupEnvironmentVariableEditor() {
        this.updateEnvironmentVariableContainer();
        
        // Sync between JSON editor and key-value pairs
        document.getElementById('environmentVariables').addEventListener('input', (e) => {
            this.populateEnvironmentVariables(e.target.value);
        });
    }

    populateEnvironmentVariables(jsonString) {
        const container = document.getElementById('envVarContainer');
        if (!jsonString || jsonString.trim() === '') {
            container.innerHTML = '<p class="text-muted">没有环境变量</p>';
            return;
        }

        try {
            const envVars = JSON.parse(jsonString);
            this.updateEnvironmentVariableContainer(envVars);
        } catch (error) {
            container.innerHTML = '<p class="text-danger">无效的JSON格式</p>';
        }
    }

    updateEnvironmentVariableContainer(envVars = {}) {
        const container = document.getElementById('envVarContainer');
        const keys = Object.keys(envVars);
        
        if (keys.length === 0) {
            container.innerHTML = '<p class="text-muted">没有环境变量</p>';
            return;
        }

        let html = '';
        keys.forEach((key, index) => {
            html += `
                <div class="input-group mb-2" data-env-index="${index}">
                    <input type="text" class="form-control env-key" placeholder="KEY" value="${key}" readonly>
                    <span class="input-group-text">=</span>
                    <input type="text" class="form-control env-value" placeholder="value" value="${envVars[key]}" readonly>
                    <button class="btn btn-outline-danger btn-sm" type="button" onclick="configEditor.removeEnvironmentVariable('${key}')">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            `;
        });

        container.innerHTML = html;
    }

    addEnvironmentVariable() {
        const key = prompt('请输入环境变量名称:');
        if (!key) return;

        const value = prompt('请输入环境变量值:');
        if (value === null) return;

        try {
            const currentVars = this.getFieldValue('environmentVariables');
            let envVars = {};
            
            if (currentVars) {
                envVars = JSON.parse(currentVars);
            }
            
            envVars[key] = value;
            
            const jsonString = JSON.stringify(envVars, null, 2);
            this.setFieldValue('environmentVariables', jsonString);
            this.populateEnvironmentVariables(jsonString);
            this.markAsDirty();
        } catch (error) {
            this.showError('添加环境变量失败');
        }
    }

    removeEnvironmentVariable(key) {
        try {
            const currentVars = this.getFieldValue('environmentVariables');
            if (!currentVars) return;

            const envVars = JSON.parse(currentVars);
            delete envVars[key];
            
            const jsonString = JSON.stringify(envVars, null, 2);
            this.setFieldValue('environmentVariables', jsonString);
            this.populateEnvironmentVariables(jsonString);
            this.markAsDirty();
        } catch (error) {
            this.showError('删除环境变量失败');
        }
    }

    validateField(field) {
        const fieldId = field.id;
        const value = field.value;
        let isValid = true;
        let errorMessage = '';

        // Clear previous validation state
        field.classList.remove('is-invalid', 'is-valid');
        const errorDiv = document.getElementById(fieldId + '-error');
        if (errorDiv) errorDiv.textContent = '';

        switch (fieldId) {
            case 'jvmOptions':
                if (value && !this.validateJvmOptions(value)) {
                    isValid = false;
                    errorMessage = '无效的JVM选项格式';
                }
                break;

            case 'gcOptions':
                if (value && !this.validateGcOptions(value)) {
                    isValid = false;
                    errorMessage = '无效的GC选项格式';
                }
                break;

            case 'systemProperties':
                if (value && !this.validateSystemProperties(value)) {
                    isValid = false;
                    errorMessage = '无效的系统属性格式';
                }
                break;

            case 'environmentVariables':
                if (value && !this.validateJson(value)) {
                    isValid = false;
                    errorMessage = '无效的JSON格式';
                }
                break;

            case 'springProperties':
                if (value && !this.validateSpringProperties(value)) {
                    isValid = false;
                    errorMessage = '无效的Spring属性格式';
                }
                break;

            case 'serverPort':
            case 'debugPort':
            case 'jmxPort':
                if (value && !this.validatePort(parseInt(value))) {
                    isValid = false;
                    errorMessage = '端口必须在1024-65535范围内';
                }
                break;

            case 'maxThreads':
                if (value && (parseInt(value) < 1 || parseInt(value) > 1000)) {
                    isValid = false;
                    errorMessage = '最大线程数必须在1-1000范围内';
                }
                break;

            case 'connectionTimeout':
                if (value && (parseInt(value) < 1000 || parseInt(value) > 300000)) {
                    isValid = false;
                    errorMessage = '连接超时必须在1000-300000毫秒范围内';
                }
                break;
        }

        // Update field validation state
        if (value) {
            field.classList.add(isValid ? 'is-valid' : 'is-invalid');
            if (!isValid && errorDiv) {
                errorDiv.textContent = errorMessage;
            }
        }

        this.validationErrors[fieldId] = isValid ? null : errorMessage;
        return isValid;
    }

    validateJvmOptions(options) {
        const optionArray = options.split(/\s+/);
        for (const option of optionArray) {
            if (option.trim() && !option.match(/^-X[ms]\d+[kmgKMG]?$|^-XX:.*$|^-D.*$/)) {
                return false;
            }
        }
        return true;
    }

    validateGcOptions(options) {
        const optionArray = options.split(/\s+/);
        for (const option of optionArray) {
            if (option.trim() && !option.match(/^-XX:[+\-]?\w+.*$/)) {
                return false;
            }
        }
        return true;
    }

    validateSystemProperties(properties) {
        const propArray = properties.split(/\s+/);
        for (const prop of propArray) {
            if (prop.trim() && !prop.match(/^-D[\w\.]+=[^\s]*$/)) {
                return false;
            }
        }
        return true;
    }

    validateSpringProperties(properties) {
        const lines = properties.split('\n');
        for (const line of lines) {
            const trimmed = line.trim();
            if (trimmed && !trimmed.startsWith('#') && !trimmed.includes('=')) {
                return false;
            }
        }
        return true;
    }

    validateJson(jsonString) {
        try {
            JSON.parse(jsonString);
            return true;
        } catch {
            return false;
        }
    }

    validatePort(port) {
        return port >= 1024 && port <= 65535;
    }

    async validateConfiguration() {
        const config = this.getFormData();
        
        try {
            const response = await this.apiCall(`/api/applications/${this.applicationId}/config/validate`, {
                method: 'POST',
                body: JSON.stringify(config)
            });

            const result = await response.json();
            
            if (result.valid) {
                this.showSuccess('配置验证通过');
                this.clearValidationErrors();
            } else {
                this.showError('配置验证失败');
                this.displayValidationErrors(result.errors);
            }
        } catch (error) {
            console.error('Validation failed:', error);
            this.showError('配置验证失败');
        }
    }

    displayValidationErrors(errors) {
        errors.forEach(error => {
            console.log('Validation error:', error);
        });
        
        // Display errors in a modal or alert
        const errorList = errors.join('\n');
        alert('配置验证错误:\n' + errorList);
    }

    clearValidationErrors() {
        document.querySelectorAll('.is-invalid').forEach(field => {
            field.classList.remove('is-invalid');
        });
        document.querySelectorAll('.invalid-feedback').forEach(errorDiv => {
            errorDiv.textContent = '';
        });
    }

    async saveConfiguration() {
        if (!this.validateAllFields()) {
            this.showError('请修正配置错误后再保存');
            return;
        }

        const config = this.getFormData();
        
        try {
            const response = await this.apiCall(`/api/applications/${this.applicationId}/config`, {
                method: 'POST',
                body: JSON.stringify(config)
            });

            const result = await response.json();
            
            if (result.success) {
                this.showSuccess('配置保存成功');
                this.isDirty = false;
                await this.loadConfigurationHistory();
            } else {
                this.showError('配置保存失败: ' + result.message);
            }
        } catch (error) {
            console.error('Save configuration failed:', error);
            this.showError('配置保存失败');
        }
    }

    async applyConfiguration() {
        await this.saveConfiguration();
        
        // Apply the latest saved configuration
        if (this.configHistory.length > 0) {
            const latestConfig = this.configHistory[0];
            
            try {
                const response = await this.apiCall(
                    `/api/applications/${this.applicationId}/config/${latestConfig.id}/apply`, 
                    { method: 'POST' }
                );

                const result = await response.json();
                
                if (result.success) {
                    this.showSuccess('配置应用成功');
                    this.activeConfig = result.config;
                    await this.loadConfigurationHistory();
                } else {
                    this.showError('配置应用失败: ' + result.message);
                }
            } catch (error) {
                console.error('Apply configuration failed:', error);
                this.showError('配置应用失败');
            }
        }
    }

    resetConfiguration() {
        if (this.isDirty) {
            if (!confirm('确定要重置配置吗？未保存的更改将丢失。')) {
                return;
            }
        }
        
        if (this.activeConfig) {
            this.populateForm(this.activeConfig);
        } else {
            this.loadDefaultConfiguration();
        }
        
        this.isDirty = false;
        this.showInfo('配置已重置');
    }

    showConfigurationPreview() {
        const config = this.getFormData();
        const preview = document.getElementById('configPreview');
        
        let html = '<h6>当前配置预览:</h6>';
        html += '<pre class="language-json"><code class="language-json">';
        html += JSON.stringify(config, null, 2);
        html += '</code></pre>';
        
        preview.innerHTML = html;
        
        // Apply syntax highlighting if Prism is available
        if (window.Prism) {
            Prism.highlightElement(preview.querySelector('code'));
        }
    }

    updatePreview() {
        // Update preview automatically if it's visible
        const preview = document.getElementById('configPreview');
        if (preview.innerHTML.includes('当前配置预览')) {
            this.showConfigurationPreview();
        }
    }

    toggleDebugOptions(enabled) {
        const debugPort = document.getElementById('debugPort');
        const debugOptions = document.getElementById('debugOptions');
        
        debugPort.disabled = !enabled;
        debugOptions.disabled = !enabled;
        
        if (!enabled) {
            debugPort.value = '';
            debugOptions.value = '';
        } else if (!debugPort.value) {
            debugPort.value = '5005';
            debugOptions.value = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005';
        }
        
        this.updatePortDisplays();
    }

    toggleJmxOptions(enabled) {
        const jmxPort = document.getElementById('jmxPort');
        const jmxAuthUser = document.getElementById('jmxAuthUser');
        
        jmxPort.disabled = !enabled;
        jmxAuthUser.disabled = !enabled;
        
        if (!enabled) {
            jmxPort.value = '';
            jmxAuthUser.value = '';
        } else if (!jmxPort.value) {
            jmxPort.value = '9999';
        }
        
        this.updatePortDisplays();
    }

    toggleHotReloadButton(supported) {
        const hotReloadBtn = document.getElementById('hotReloadBtn');
        hotReloadBtn.style.display = supported ? 'inline-block' : 'none';
    }

    updatePortDisplays() {
        const debugPort = this.getFieldValue('debugPort');
        const jmxPort = this.getFieldValue('jmxPort');
        
        const debugPortDisplay = document.getElementById('debugPortDisplay');
        const jmxPortDisplay = document.getElementById('jmxPortDisplay');
        
        if (debugPortDisplay) debugPortDisplay.textContent = debugPort || '5005';
        if (jmxPortDisplay) jmxPortDisplay.textContent = jmxPort || '9999';
    }

    validateAllFields() {
        const form = document.getElementById('configForm');
        const fields = form.querySelectorAll('input, textarea, select');
        let allValid = true;
        
        fields.forEach(field => {
            if (!this.validateField(field)) {
                allValid = false;
            }
        });
        
        return allValid;
    }

    updateTemplateList() {
        const templateList = document.getElementById('templateList');
        let html = '';
        
        // Predefined templates
        Object.keys(this.predefinedTemplates).forEach(name => {
            const template = this.predefinedTemplates[name];
            html += `
                <a href="#" class="list-group-item list-group-item-action" 
                   onclick="configEditor.loadTemplate('${name}', true)">
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1">${name}</h6>
                        <small class="text-muted">预定义</small>
                    </div>
                    <p class="mb-1">${template.description || ''}</p>
                </a>
            `;
        });
        
        // Custom templates
        this.templates.forEach(template => {
            html += `
                <a href="#" class="list-group-item list-group-item-action" 
                   onclick="configEditor.loadTemplate('${template.templateName}', false)">
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1">${template.templateName}</h6>
                        <small class="text-muted">自定义</small>
                    </div>
                    <p class="mb-1">${template.description || ''}</p>
                </a>
            `;
        });
        
        if (html === '') {
            html = '<div class="p-3 text-muted text-center">暂无模板</div>';
        }
        
        templateList.innerHTML = html;
    }

    updateHistoryList() {
        const historyList = document.getElementById('historyList');
        let html = '';
        
        this.configHistory.forEach(config => {
            const isActive = config.isActive ? ' (活动)' : '';
            const statusBadge = config.isActive ? 'bg-success' : 'bg-secondary';
            
            html += `
                <a href="#" class="list-group-item list-group-item-action" 
                   onclick="configEditor.loadHistoryConfig(${config.id})">
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1">
                            版本 ${config.configVersion || '1.0.0'}
                            <span class="badge ${statusBadge}">${config.configType}${isActive}</span>
                        </h6>
                        <small class="text-muted">${this.formatDate(config.createdAt)}</small>
                    </div>
                    <p class="mb-1">${config.description || '无描述'}</p>
                    <small class="text-muted">创建者: ${config.createdBy?.username || '未知'}</small>
                </a>
            `;
        });
        
        if (html === '') {
            html = '<div class="p-3 text-muted text-center">暂无历史记录</div>';
        }
        
        historyList.innerHTML = html;
    }

    loadTemplate(templateName, isPredefined) {
        if (this.isDirty && !confirm('当前有未保存的更改，确定要加载模板吗？')) {
            return;
        }
        
        let template;
        if (isPredefined) {
            template = this.predefinedTemplates[templateName];
        } else {
            template = this.templates.find(t => t.templateName === templateName);
        }
        
        if (template) {
            this.populateForm(template);
            this.showSuccess(`已加载模板: ${templateName}`);
        }
    }

    loadHistoryConfig(configId) {
        if (this.isDirty && !confirm('当前有未保存的更改，确定要加载历史配置吗？')) {
            return;
        }
        
        const config = this.configHistory.find(c => c.id === configId);
        if (config) {
            this.populateForm(config);
            this.showSuccess(`已加载历史配置: 版本 ${config.configVersion}`);
        }
    }

    async hotReloadConfiguration() {
        if (this.configHistory.length === 0) {
            this.showError('没有可热重载的配置');
            return;
        }
        
        const latestConfig = this.configHistory[0];
        
        try {
            const response = await this.apiCall(
                `/api/applications/${this.applicationId}/config/${latestConfig.id}/hot-reload`,
                { method: 'POST' }
            );

            const result = await response.json();
            
            if (result.success) {
                this.showSuccess('配置热重载成功');
            } else {
                this.showWarning('热重载失败，需要重启应用: ' + result.message);
            }
        } catch (error) {
            console.error('Hot reload failed:', error);
            this.showError('热重载失败');
        }
    }

    async createBackup() {
        const description = prompt('请输入备份描述:', '手动备份 - ' + new Date().toLocaleString());
        if (description === null) return;
        
        try {
            const response = await this.apiCall(
                `/api/applications/${this.applicationId}/config/backup?description=${encodeURIComponent(description)}`,
                { method: 'POST' }
            );

            const result = await response.json();
            
            if (result.success) {
                this.showSuccess('备份创建成功');
                await this.loadConfigurationHistory();
            } else {
                this.showError('备份创建失败: ' + result.message);
            }
        } catch (error) {
            console.error('Create backup failed:', error);
            this.showError('备份创建失败');
        }
    }

    // Modal management methods
    showTemplateModal() {
        const modal = new bootstrap.Modal(document.getElementById('templateModal'));
        modal.show();
    }

    showImportModal() {
        const modal = new bootstrap.Modal(document.getElementById('importModal'));
        modal.show();
    }

    showExportModal() {
        const modal = new bootstrap.Modal(document.getElementById('exportModal'));
        modal.show();
    }

    showSaveTemplateModal() {
        const modal = new bootstrap.Modal(document.getElementById('saveTemplateModal'));
        modal.show();
    }

    showCompareModal() {
        this.populateCompareSelects();
        const modal = new bootstrap.Modal(document.getElementById('compareModal'));
        modal.show();
    }

    populateCompareSelects() {
        const select1 = document.getElementById('compareConfig1');
        const select2 = document.getElementById('compareConfig2');
        
        let options = '<option value="">选择配置...</option>';
        this.configHistory.forEach(config => {
            const label = `版本 ${config.configVersion} - ${config.configType} - ${this.formatDate(config.createdAt)}`;
            options += `<option value="${config.id}">${label}</option>`;
        });
        
        select1.innerHTML = options;
        select2.innerHTML = options;
    }

    async importConfiguration() {
        const fileInput = document.getElementById('configFileInput');
        const format = document.getElementById('importFormat').value;
        
        if (!fileInput.files[0]) {
            this.showError('请选择配置文件');
            return;
        }
        
        const file = fileInput.files[0];
        const formData = new FormData();
        formData.append('file', file);
        formData.append('format', format);
        
        try {
            const response = await fetch(`/api/applications/${this.applicationId}/config/import`, {
                method: 'POST',
                headers: {
                    [this.csrfHeader]: this.csrfToken
                },
                body: formData
            });

            const result = await response.json();
            
            if (result.success) {
                this.showSuccess('配置导入成功');
                this.populateForm(result.config);
                bootstrap.Modal.getInstance(document.getElementById('importModal')).hide();
                await this.loadConfigurationHistory();
            } else {
                this.showError('配置导入失败: ' + result.message);
            }
        } catch (error) {
            console.error('Import configuration failed:', error);
            this.showError('配置导入失败');
        }
    }

    exportConfiguration() {
        const format = document.getElementById('exportFormat').value;
        const configType = document.getElementById('exportConfigSelect').value;
        
        let configId;
        if (configType === 'active' && this.activeConfig) {
            configId = this.activeConfig.id;
        } else if (configType === 'current') {
            // Export current form data as new config
            this.showError('请先保存当前配置再导出');
            return;
        } else {
            this.showError('没有可导出的配置');
            return;
        }
        
        const exportUrl = `/api/applications/${this.applicationId}/config/${configId}/export/${format}`;
        window.open(exportUrl, '_blank');
        
        bootstrap.Modal.getInstance(document.getElementById('exportModal')).hide();
        this.showSuccess('配置导出已开始');
    }

    async saveTemplate() {
        const templateName = document.getElementById('templateName').value;
        const templateDescription = document.getElementById('templateDescription').value;
        
        if (!templateName) {
            this.showError('请输入模板名称');
            return;
        }
        
        const config = this.getFormData();
        
        try {
            const response = await this.apiCall(
                `/api/applications/${this.applicationId}/config/templates`,
                {
                    method: 'POST',
                    body: JSON.stringify({
                        config: config,
                        templateName: templateName,
                        description: templateDescription
                    })
                }
            );

            const result = await response.json();
            
            if (result.success) {
                this.showSuccess('模板保存成功');
                bootstrap.Modal.getInstance(document.getElementById('saveTemplateModal')).hide();
                await this.loadTemplates();
                
                // Clear form
                document.getElementById('templateName').value = '';
                document.getElementById('templateDescription').value = '';
            } else {
                this.showError('模板保存失败: ' + result.message);
            }
        } catch (error) {
            console.error('Save template failed:', error);
            this.showError('模板保存失败');
        }
    }

    async compareConfigurations() {
        const config1Id = document.getElementById('compareConfig1').value;
        const config2Id = document.getElementById('compareConfig2').value;
        
        if (!config1Id || !config2Id) {
            this.showError('请选择两个要比较的配置');
            return;
        }
        
        if (config1Id === config2Id) {
            this.showError('请选择不同的配置进行比较');
            return;
        }
        
        try {
            const response = await this.apiCall(
                `/api/applications/${this.applicationId}/config/${config1Id}/compare/${config2Id}`
            );

            const result = await response.json();
            
            if (result.success) {
                this.displayConfigComparison(result);
            } else {
                this.showError('配置比较失败: ' + result.message);
            }
        } catch (error) {
            console.error('Compare configurations failed:', error);
            this.showError('配置比较失败');
        }
    }

    displayConfigComparison(result) {
        const compareResult = document.getElementById('compareResult');
        const differences = result.differences;
        
        if (Object.keys(differences).length === 0) {
            compareResult.innerHTML = '<div class="alert alert-info">两个配置相同，没有差异</div>';
            return;
        }
        
        let html = '<h6>配置差异:</h6>';
        html += '<div class="table-responsive">';
        html += '<table class="table table-sm table-striped">';
        html += '<thead><tr><th>配置项</th><th>配置1</th><th>配置2</th></tr></thead>';
        html += '<tbody>';
        
        Object.keys(differences).forEach(key => {
            const diff = differences[key];
            html += `
                <tr>
                    <td><strong>${key}</strong></td>
                    <td class="text-danger">${this.formatValue(diff.old)}</td>
                    <td class="text-success">${this.formatValue(diff.new)}</td>
                </tr>
            `;
        });
        
        html += '</tbody></table></div>';
        compareResult.innerHTML = html;
    }

    formatValue(value) {
        if (value === null || value === undefined) {
            return '<em>未设置</em>';
        }
        if (typeof value === 'string' && value.length > 100) {
            return value.substring(0, 100) + '...';
        }
        return value.toString();
    }

    markAsDirty() {
        this.isDirty = true;
    }

    formatDate(dateString) {
        if (!dateString) return '未知';
        return new Date(dateString).toLocaleString('zh-CN');
    }

    // Utility methods
    async apiCall(url, options = {}) {
        const defaultOptions = {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                [this.csrfHeader]: this.csrfToken
            }
        };
        
        const mergedOptions = { ...defaultOptions, ...options };
        
        return fetch(url, mergedOptions);
    }

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Notification methods
    showSuccess(message) {
        this.showNotification(message, 'success');
    }

    showError(message) {
        this.showNotification(message, 'error');
    }

    showWarning(message) {
        this.showNotification(message, 'warning');
    }

    showInfo(message) {
        this.showNotification(message, 'info');
    }

    showNotification(message, type) {
        // Simple notification implementation
        const alertClass = {
            success: 'alert-success',
            error: 'alert-danger',
            warning: 'alert-warning',
            info: 'alert-info'
        }[type] || 'alert-info';
        
        const notification = document.createElement('div');
        notification.className = `alert ${alertClass} alert-dismissible fade show position-fixed`;
        notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(notification);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    }
}

// Initialize the configuration editor when the page loads
let configEditor;
document.addEventListener('DOMContentLoaded', function() {
    configEditor = new ConfigurationEditor();
});

// Export for global access
window.configEditor = configEditor;