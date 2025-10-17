import re

def check_main_layout():
    filepath = r'E:\work\code\internalpaas\src\main\resources\templates\main-layout.html'
    errors = []
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        print(f"File size: {len(content)} characters")
        
        # 检查标签配对
        stack = []
        
        # 自闭合标签
        self_closing_tags = {
            'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input',
            'link', 'meta', 'param', 'source', 'track', 'wbr'
        }
        
        # 找到所有标签
        tag_pattern = r'<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*?(/?)>'
        matches = list(re.finditer(tag_pattern, content, re.IGNORECASE))
        
        print(f"Found {len(matches)} HTML tags")
        
        for match in matches:
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
        
        # 检查属性值未加引号（只检查前1000行避免太多输出）
        lines = content.split('\n')[:1000]
        first_1000_lines = '\n'.join(lines)
        
        attr_pattern = r'<[^>]*\s+([a-zA-Z-]+)=([^"\'>\s][^>\s]*)'
        for match in re.finditer(attr_pattern, first_1000_lines):
            line_num = first_1000_lines[:match.start()].count('\n') + 1
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

print('=== Checking main-layout.html ===')
errors = check_main_layout()

if errors:
    print(f"Found {len(errors)} errors:")
    for error in errors[:20]:  # 只显示前20个错误
        print(f'ERROR: {error}')
    if len(errors) > 20:
        print(f'... and {len(errors) - 20} more errors')
else:
    print('No HTML syntax errors found.')