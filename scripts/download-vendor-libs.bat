@echo off
echo 正在创建vendor目录结构...
mkdir "src\main\resources\static\vendor\bootstrap" 2>nul
mkdir "src\main\resources\static\vendor\chartjs" 2>nul
mkdir "src\main\resources\static\vendor\xterm" 2>nul
mkdir "src\main\resources\static\vendor\prism" 2>nul
mkdir "src\main\resources\static\vendor\flatpickr" 2>nul
mkdir "src\main\resources\static\vendor\sockjs" 2>nul
mkdir "src\main\resources\static\vendor\hammerjs" 2>nul

echo 下载 Bootstrap 5.1.3...
curl -o "src\main\resources\static\vendor\bootstrap\bootstrap.min.css" "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css"
curl -o "src\main\resources\static\vendor\bootstrap\bootstrap.bundle.min.js" "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"

echo 下载 Bootstrap 5.3.0...
curl -o "src\main\resources\static\vendor\bootstrap\bootstrap-5.3.0.min.css" "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css"
curl -o "src\main\resources\static\vendor\bootstrap\bootstrap-5.3.0.bundle.min.js" "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"

echo 下载 Bootstrap 5.3.2...
curl -o "src\main\resources\static\vendor\bootstrap\bootstrap-5.3.2.min.css" "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
curl -o "src\main\resources\static\vendor\bootstrap\bootstrap-5.3.2.bundle.min.js" "https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"

echo 下载 Chart.js...
curl -o "src\main\resources\static\vendor\chartjs\chart.min.css" "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.min.css"
curl -o "src\main\resources\static\vendor\chartjs\chart.umd.js" "https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js"
curl -o "src\main\resources\static\vendor\chartjs\chartjs-adapter-date-fns.bundle.min.js" "https://cdn.jsdelivr.net/npm/chartjs-adapter-date-fns/dist/chartjs-adapter-date-fns.bundle.min.js"

echo 下载 XTerm.js...
curl -o "src\main\resources\static\vendor\xterm\xterm.css" "https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.css"
curl -o "src\main\resources\static\vendor\xterm\xterm.js" "https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.js"
curl -o "src\main\resources\static\vendor\xterm\xterm-addon-fit.js" "https://cdn.jsdelivr.net/npm/xterm-addon-fit@0.8.0/lib/xterm-addon-fit.js"
curl -o "src\main\resources\static\vendor\xterm\xterm-addon-web-links.js" "https://cdn.jsdelivr.net/npm/xterm-addon-web-links@0.9.0/lib/xterm-addon-web-links.js"
curl -o "src\main\resources\static\vendor\xterm\xterm-addon-search.js" "https://cdn.jsdelivr.net/npm/xterm-addon-search@0.13.0/lib/xterm-addon-search.js"

echo 下载 Prism.js...
curl -o "src\main\resources\static\vendor\prism\prism.min.css" "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css"
curl -o "src\main\resources\static\vendor\prism\prism-tomorrow.min.css" "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css"
curl -o "src\main\resources\static\vendor\prism\prism.min.js" "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"
curl -o "src\main\resources\static\vendor\prism\prism-json.min.js" "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-json.min.js"
curl -o "src\main\resources\static\vendor\prism\prism-yaml.min.js" "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-yaml.min.js"

echo 下载 Flatpickr...
curl -o "src\main\resources\static\vendor\flatpickr\flatpickr.min.css" "https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css"

echo 下载 WebSocket 库...
curl -o "src\main\resources\static\vendor\sockjs\sockjs.min.js" "https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"
curl -o "src\main\resources\static\vendor\sockjs\stomp.min.js" "https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"

echo 下载 HammerJS...
curl -o "src\main\resources\static\vendor\hammerjs\hammer.min.js" "https://cdn.jsdelivr.net/npm/hammerjs@2.0.8"

echo 所有依赖库下载完成！
pause