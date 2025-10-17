import re
import os

def comprehensive_html_check():
    """全面检查HTML文件的语法错误"""
    
    template_dir = r'E:\work\code\internalpaas\src\main\resources\templates'
    all_errors = {}
    
    # 需要检查的HTML文件
    html_files = []
    for root, dirs, files in os.walk(template_dir):
        for file in files:
            if file.endswith('.html'):
                html_files.append(os.path.join(root, file))
    
    print(f"Found {len(html_files)} HTML files to check")
    
    for filepath in html_files:
        relative_path = os.path.relpath(filepath, template_dir)
        print(f"\nChecking: {relative_path}")
        
        errors = check_single_file(filepath)
        if errors:
            all_errors[relative_path] = errors
            print(f"  Found {len(errors)} errors")
        else:
            print("  No errors found")
    
    return all_errors

def check_single_file(filepath):
    """检查单个HTML文件的语法错误"""
    errors = []
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        lines = content.split('\n')
        
        # 1. 检查标签配对
        tag_errors = check_tag_matching(content)
        errors.extend(tag_errors)
        
        # 2. 检查属性引号
        quote_errors = check_attribute_quotes(content)
        errors.extend(quote_errors)
        
        # 3. 检查重复ID
        id_errors = check_duplicate_ids(content)
        errors.extend(id_errors)
        
        # 4. 检查常见的HTML拼写错误
        spelling_errors = check_tag_spelling(content)
        errors.extend(spelling_errors)
        
        # 5. 检查未闭合的注释
        comment_errors = check_html_comments(content)
        errors.extend(comment_errors)
        
    except Exception as e:
        errors.append(f"File reading error: {e}")
    
    return errors

def check_tag_matching(content):
    """检查HTML标签配对"""
    errors = []
    stack = []
    
    # 自闭合标签
    self_closing_tags = {
        'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input',
        'link', 'meta', 'param', 'source', 'track', 'wbr'
    }
    
    # 查找所有HTML标签
    tag_pattern = r'<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*?(/?)>'
    for match in re.finditer(tag_pattern, content, re.IGNORECASE):
        is_closing = bool(match.group(1))
        tag_name = match.group(2).lower()
        is_self_closing = bool(match.group(3))
        line_num = content[:match.start()].count('\n') + 1
        
        if is_closing:
            if not stack:
                errors.append(f"Line {line_num}: Closing tag </{tag_name}> without matching opening tag")
            else:
                last_tag, last_line = stack.pop()
                if last_tag != tag_name:
                    errors.append(f"Line {line_num}: Mismatched closing tag </{tag_name}>, expected </{last_tag}> (opened at line {last_line})")
        elif tag_name not in self_closing_tags and not is_self_closing:
            stack.append((tag_name, line_num))
    
    # 检查未闭合的标签
    while stack:
        tag_name, line_num = stack.pop()
        errors.append(f"Line {line_num}: Unclosed tag <{tag_name}>")
    
    return errors

def check_attribute_quotes(content):
    """检查属性值是否正确引用"""
    errors = []
    
    # 查找属性值未加引号的情况
    # 这个正则需要小心，避免误报
    attr_pattern = r'<[^>]*\s+([a-zA-Z-]+)=([^"\'>\s]\S*)'
    for match in re.finditer(attr_pattern, content):
        line_num = content[:match.start()].count('\n') + 1
        attr_name = match.group(1)
        attr_value = match.group(2)
        
        # 过滤掉一些特殊情况
        if not attr_value.startswith(('{{', '${', 'javascript:')):
            errors.append(f"Line {line_num}: Attribute '{attr_name}' value '{attr_value}' should be quoted")
    
    return errors

def check_duplicate_ids(content):
    """检查重复的ID属性"""
    errors = []
    ids = {}
    
    # 查找所有ID属性，但排除模板变量
    id_pattern = r'id\s*=\s*["\']([^"\']+)["\']'
    for match in re.finditer(id_pattern, content, re.IGNORECASE):
        id_value = match.group(1)
        line_num = content[:match.start()].count('\n') + 1
        
        # 忽略Thymeleaf模板变量
        if '${' not in id_value and '#{' not in id_value:
            if id_value in ids:
                errors.append(f"Line {line_num}: Duplicate ID '{id_value}' (first seen at line {ids[id_value]})")
            else:
                ids[id_value] = line_num
    
    return errors

def check_tag_spelling(content):
    """检查常见的HTML标签拼写错误"""
    errors = []
    
    # 常见的拼写错误
    common_mistakes = {
        'dvi': 'div',
        'spna': 'span', 
        'from': 'form',
        'buttom': 'button',
        'imput': 'input',
        'lable': 'label'
    }
    
    for wrong, correct in common_mistakes.items():
        pattern = f'<{wrong}[>\\s]'
        matches = re.finditer(pattern, content, re.IGNORECASE)
        for match in matches:
            line_num = content[:match.start()].count('\n') + 1
            errors.append(f"Line {line_num}: Misspelled tag '<{wrong}>', did you mean '<{correct}>'?")
    
    return errors

def check_html_comments(content):
    """检查HTML注释是否正确闭合"""
    errors = []
    
    # 查找未闭合的HTML注释
    comment_starts = []
    for match in re.finditer(r'<!--', content):
        line_num = content[:match.start()].count('\n') + 1
        comment_starts.append(line_num)
    
    comment_ends = []
    for match in re.finditer(r'-->', content):
        line_num = content[:match.start()].count('\n') + 1
        comment_ends.append(line_num)
    
    if len(comment_starts) != len(comment_ends):
        errors.append(f"Mismatched HTML comments: {len(comment_starts)} opening, {len(comment_ends)} closing")
    
    return errors

def generate_report(all_errors):
    """生成详细的错误报告"""
    report = []
    report.append("=" * 80)
    report.append("HTML 语法错误检查报告")
    report.append("=" * 80)
    report.append("")
    
    if not all_errors:
        report.append("✅ 未发现HTML语法错误！所有文件都通过了检查。")
        return '\n'.join(report)
    
    total_errors = sum(len(errors) for errors in all_errors.values())
    report.append(f"❌ 发现 {len(all_errors)} 个文件存在问题，总共 {total_errors} 个错误")
    report.append("")
    
    for filepath, errors in all_errors.items():
        report.append(f"📁 文件: {filepath}")
        report.append("-" * 50)
        
        for error in errors:
            report.append(f"  ❌ {error}")
        
        report.append("")
    
    report.append("=" * 80)
    report.append("修复建议:")
    report.append("=" * 80)
    report.append("")
    report.append("1. 属性值引号问题:")
    report.append("   - 确保所有HTML属性值都用双引号或单引号包围")
    report.append("   - 例如: content=\"width=device-width, initial-scale=1.0\"")
    report.append("")
    report.append("2. 重复ID问题:")
    report.append("   - 检查页面中是否有重复的id属性")
    report.append("   - 如果使用Thymeleaf模板，确保动态生成的ID是唯一的")
    report.append("")
    report.append("3. 标签配对问题:")
    report.append("   - 确保每个开始标签都有对应的结束标签")
    report.append("   - 自闭合标签不需要结束标签")
    report.append("")
    
    return '\n'.join(report)

if __name__ == "__main__":
    print("开始HTML语法检查...")
    all_errors = comprehensive_html_check()
    
    report = generate_report(all_errors)
    
    # 保存报告到文件
    report_file = r'E:\work\code\internalpaas\HTML_Error_Report.txt'
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\n报告已保存到: {report_file}")
    print("\n" + "=" * 50)
    print(report)