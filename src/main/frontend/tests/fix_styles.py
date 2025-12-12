#!/usr/bin/env python3
"""
修复组件测试中的CSS Module引用
将 styles['className'] 替换为 [class*="className"]
"""
import re
import sys

def fix_test_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # 替换 container.querySelector(`.${styles['xxx']}`)
    content = re.sub(
        r"container\.querySelector\(`\.\$\{styles\['([^']+)'\]\}`\)",
        r"container.querySelector('[class*=\"\1\"]')",
        content
    )

    # 替换 .toContain(styles['xxx'])
    content = re.sub(
        r"\.toContain\(styles\['([^']+)'\]\)",
        r".toMatch(/\1/)",
        content
    )

    # 替换 .not.toContain(styles['xxx'])
    content = re.sub(
        r"\.not\.toContain\(styles\['([^']+)'\]\)",
        r".not.toMatch(/\1/)",
        content
    )

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Fixed {filepath}")

if __name__ == '__main__':
    if len(sys.argv) > 1:
        fix_test_file(sys.argv[1])
    else:
        print("Usage: python fix_styles.py <test_file.tsx>")
