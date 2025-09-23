(function () {
    const pageRoot = document.querySelector('.auth-page');
    if (!pageRoot) {
        return;
    }

    const form = document.querySelector('.auth-form');
    const submitButton = form ? form.querySelector('.submit-btn') : null;
    const passwordField = form ? form.querySelector('#password') : null;
    const passwordToggle = form ? form.querySelector('[data-action="toggle-password"]') : null;
    const capsLockHint = form ? form.querySelector('[data-capslock-hint]') : null;
    const inputs = form ? Array.from(form.querySelectorAll('input')) : [];

    function updateFieldState(input) {
        const field = input.closest('.form-field');
        if (!field) {
            return;
        }
        if (input.value) {
            field.classList.add('has-value');
        } else {
            field.classList.remove('has-value');
        }
    }

    inputs.forEach(function (input) {
        const field = input.closest('.form-field');
        if (!field) {
            return;
        }
        input.addEventListener('focus', function () {
            field.classList.add('is-focused');
        });
        input.addEventListener('blur', function () {
            field.classList.remove('is-focused');
            updateFieldState(input);
        });
        updateFieldState(input);
    });

    if (passwordToggle && passwordField) {
        passwordToggle.addEventListener('click', function () {
            const isHidden = passwordField.getAttribute('type') === 'password';
            passwordField.setAttribute('type', isHidden ? 'text' : 'password');
            passwordToggle.textContent = isHidden ? '隐藏' : '显示';
        });
    }

    if (passwordField && capsLockHint) {
        var capsLockActive = false;
        var toggleHint = function (active) {
            capsLockActive = active;
            capsLockHint.hidden = !active;
        };
        passwordField.addEventListener('keydown', function (event) {
            if (typeof event.getModifierState === 'function') {
                toggleHint(event.getModifierState('CapsLock'));
            }
        });
        passwordField.addEventListener('keyup', function (event) {
            if (typeof event.getModifierState === 'function') {
                toggleHint(event.getModifierState('CapsLock'));
            }
        });
        passwordField.addEventListener('blur', function () {
            toggleHint(false);
        });
    }

    if (form && submitButton) {
        form.addEventListener('submit', function (event) {
            if (typeof form.checkValidity === 'function' && !form.checkValidity()) {
                event.preventDefault();
                if (typeof form.reportValidity === 'function') {
                    form.reportValidity();
                }
                return;
            }

            form.classList.add('is-submitting');
            submitButton.setAttribute('aria-busy', 'true');
            submitButton.disabled = true;
        });

        window.addEventListener('pageshow', function () {
            form.classList.remove('is-submitting');
            submitButton.disabled = false;
            submitButton.removeAttribute('aria-busy');
        });
    }

    const statusIndicator = document.querySelector('[data-status-indicator]');
    if (statusIndicator) {
        const online = navigator.onLine;
        statusIndicator.dataset.state = online ? 'online' : 'offline';
        statusIndicator.textContent = online ? '服务正常' : '网络不可用';
        window.addEventListener('online', function () {
            statusIndicator.dataset.state = 'online';
            statusIndicator.textContent = '服务正常';
        });
        window.addEventListener('offline', function () {
            statusIndicator.dataset.state = 'offline';
            statusIndicator.textContent = '网络不可用';
        });
    }

    const isRegisterPage = pageRoot.classList.contains('register-view');
    if (isRegisterPage && form) {
        var passwordConfirm = form.querySelector('#confirmPassword');
        var passwordPrimary = passwordField;
        var syncValidity = function () {
            if (!passwordConfirm || !passwordPrimary) {
                return;
            }
            if (passwordConfirm.value && passwordPrimary.value !== passwordConfirm.value) {
                passwordConfirm.setCustomValidity('两次输入的密码不一致');
            } else {
                passwordConfirm.setCustomValidity('');
            }
        };
        if (passwordConfirm) {
            passwordConfirm.addEventListener('input', syncValidity);
        }
        if (passwordPrimary) {
            passwordPrimary.addEventListener('input', syncValidity);
        }
    }

    const assistLinks = document.querySelectorAll('.assist-link[data-action]');
    assistLinks.forEach(function (link) {
        const action = link.getAttribute('data-action');
        link.addEventListener('click', function (event) {
            event.preventDefault();
            if (action === 'helpdesk') {
                window.location.href = 'mailto:ops-support@internal.example.com?subject=Dev%20Debug%20Platform%20密码重置请求';
            } else if (action === 'status') {
                if (!navigator.onLine) {
                    window.alert('当前网络不可用，无法打开平台公告。');
                    return;
                }
                window.open('/status', '_blank');
            }
        });
    });
})();
