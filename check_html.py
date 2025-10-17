import re
import sys

def check_html_file(filepath):
    errors = []
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 检查标签配对
        stack = []
        line_num = 1
        
        # 自闭合标签
        self_closing_tags = {
            'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input',
            'link', 'meta', 'param', 'source', 'track', 'wbr'
        }
        
        # 找到所有标签
        tag_pattern = r'<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*?(/?)>'
        for match in re.finditer(tag_pattern, content, re.IGNORECASE):
            is_closing = bool(match.group(1))
            tag_name = match.group(2).lower()
            is_self_closing = bool(match.group(3))
            
            # 计算行号
            line_num = content[:match.start()].count('\n') + 1
            
            if is_closing:
                if not stack:
                    errors.append(f'Line {line_num}: Closing tag </{tag_name}> without matching opening tag')
                else:
                    last_tag, last_line = stack.pop()
                    if last_tag != tag_name:
                        errors.append(f'Line {line_num}: Mismatched closing tag </{tag_name}>, expected </{last_tag}> (opened at line {last_line})')
            elif tag_name not in self_closing_tags and not is_self_closing:
                stack.append((tag_name, line_num))
        
        # 检查未闭合的标签
        while stack:
            tag_name, line_num = stack.pop()
            errors.append(f'Line {line_num}: Unclosed tag <{tag_name}>')
        
        # 检查属性值未加引号
        attr_pattern = r'<[^>]*\s+([a-zA-Z-]+)=([^\"\'>\s][^>\s]*)'
        for match in re.finditer(attr_pattern, content):
            line_num = content[:match.start()].count('\n') + 1
            attr_name = match.group(1)
            attr_value = match.group(2)
            errors.append(f'Line {line_num}: Attribute "{attr_name}" value "{attr_value}" should be quoted')
        
        # 检查重复的id属性
        id_pattern = r'id=["\']([^"\']*)["\']'
        ids = {}
        for match in re.finditer(id_pattern, content, re.IGNORECASE):
            id_value = match.group(1)
            line_num = content[:match.start()].count('\n') + 1
            if id_value in ids:
                errors.append(f'Line {line_num}: Duplicate ID "{id_value}" (first seen at line {ids[id_value]})')
            else:
                ids[id_value] = line_num
                
    except Exception as e:
        errors.append(f'Error reading file: {e}')
    
    return errors

# 检查指定的文件
files_to_check = [
    r'E:\work\code\internalpaas\src\main\resources\templates\applications.html',
    r'E:\work\code\internalpaas\src\main\resources\templates\admin\config-editor.html'
]

for filepath in files_to_check:
    filename = filepath.split('\\')[-1]
    print(f'\n=== Checking {filename} ===')
    errors = check_html_file(filepath)
    if errors:
        for error in errors:
            print(f'ERROR: {error}')
    else:
        print('No HTML syntax errors found.')