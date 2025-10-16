#!/usr/bin/env bash
# system-audit-demo.sh
# Demo script: interactive menu to collect comprehensive system information

set -euo pipefail

pause() {
  echo
  read -rp "Press Enter to return to menu..." _
}

show_system() {
  echo "=== System Overview ==="
  cat /etc/os-release || true
  echo
  uname -a
  echo
  hostnamectl || true
  echo
  echo "-- Uptime --"
  uptime
  who -b || true
  echo
  echo "-- Timezone --"
  timedatectl || true
}

show_hardware() {
  echo "=== Hardware Resources ==="
  echo "-- CPU --"
  lscpu
  echo
  echo "-- Memory --"
  free -h
  echo
  echo "-- Block Devices --"
  lsblk -o NAME,FSTYPE,SIZE,MOUNTPOINT
  echo
  echo "-- Filesystem Usage --"
  df -hT
  echo
  echo "-- Detailed Hardware (sudo) --"
  sudo lshw -short 2>/dev/null || echo "(lshw unavailable or requires sudo)"
  echo
  sudo dmidecode 2>/dev/null | head || echo "(dmidecode unavailable or requires sudo)"
}

show_network() {
  echo "=== Network Configuration ==="
  echo "-- Interfaces --"
  ip addr show
  echo
  echo "-- Routing Table --"
  ip route
  echo
  echo "-- Interface Statistics --"
  ip -s link
  echo
  echo "-- Firewall Status --"
  (sudo firewall-cmd --list-all 2>/dev/null || sudo ufw status 2>/dev/null || echo "(no firewall tool detected)")
  echo
  echo "-- Listening Ports --"
  sudo ss -tulpn
  echo
  echo "-- DNS / Resolver --"
  cat /etc/resolv.conf
  getent hosts $(hostname) || true
}

show_services() {
  echo "=== System State & Services ==="
  echo "-- Running systemd units --"
  systemctl list-units --type=service --state=running
  echo
  echo "-- Failed units --"
  systemctl --failed || true
  echo
  echo "-- Top CPU processes --"
  ps aux --sort=-%cpu | head
  echo
  echo "-- Top Memory processes --"
  ps aux --sort=-%mem | head
  echo
  echo "-- Load (top snapshot) --"
  top -b -n1 | head -n 20
  echo
  echo "-- Cron jobs --"
  crontab -l 2>/dev/null || echo "(no user crontab)"
  sudo ls /etc/cron.* -R
}

show_security() {
  echo "=== Security & User Environment ==="
  echo "-- Recent logins --"
  last -a | head
  echo
  who
  echo
  echo "-- Local users (UID >= 1000) --"
  awk -F: '$3 >= 1000 {print $1,$7}' /etc/passwd
  echo
  echo "-- Sudoers --"
  sudo grep -Ev '^#|^$' /etc/sudoers /etc/sudoers.d/* 2>/dev/null || echo "(sudoers entries not accessible)"
  echo
  echo "-- SELinux/AppArmor/Audit --"
  getenforce 2>/dev/null || echo "(SELinux not present)"
  sudo aa-status 2>/dev/null || echo "(AppArmor not present)"
  sudo ausearch -m avc -ts recent 2>/dev/null | tail || echo "(audit logs unavailable)"
  echo
  echo "-- Kernel security settings --"
  sudo sysctl net.ipv4.conf.all.rp_filter
  sudo sysctl net.ipv4.ip_forward
}

show_logs() {
  echo "=== Logs & Troubleshooting ==="
  echo "-- Journal (errors) --"
  sudo journalctl -p err -n 50
  echo
  echo "-- Syslog/messages tail --"
  sudo tail -n 100 /var/log/syslog 2>/dev/null || sudo tail -n 100 /var/log/messages 2>/dev/null || echo "(syslog/messages not found)"
  echo
  echo "-- Kernel messages --"
  sudo dmesg | tail
  echo
  echo "-- Service example: sshd (last hour) --"
  sudo journalctl -u ssh -u sshd --since "1 hour ago" | tail
}

menu() {
  clear
  cat <<'EOF'
==== System Audit Demo ====
1) System Overview
2) Hardware Resources
3) Network Configuration
4) System State & Services
5) Security & User Environment
6) Logs & Troubleshooting
0) Exit
EOF
  read -rp "Select an option: " choice
  case "$choice" in
    1) show_system; pause ;;
    2) show_hardware; pause ;;
    3) show_network; pause ;;
    4) show_services; pause ;;
    5) show_security; pause ;;
    6) show_logs; pause ;;
    0) exit 0 ;;
    *) echo "Invalid option"; sleep 1 ;;
  esac
}

while true; do
  menu
done
