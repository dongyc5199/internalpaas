import "./styles/main.css";
import "./styles/ssh-config-import-wizard-v2.css";
import "./modules/server-group-management";
import "./modules/server-modal";
import "./modules/server-import-modal";
import "./modules/SSHConfigImportWizard";
import "./modules/theme";
import "./modules/dashboard";

document.addEventListener("DOMContentLoaded", () => {
    const message = document.querySelector("[data-dev-shell-message]");
    if (message) {
        message.textContent = "前端工程化脚手架已初始化。";
    }
});
